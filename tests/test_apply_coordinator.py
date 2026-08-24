import subprocess
import tempfile
import unittest
from pathlib import Path


class ApplyCoordinatorTests(unittest.TestCase):
    def test_apply_persists_backup_before_remote_and_rolls_back_on_state_failure(self):
        root = Path(__file__).resolve().parents[1]
        folder_dir = root / "overlay/java/org/telegram/teleflow/folders"
        sources = [
            folder_dir / "FolderServerSnapshot.java",
            folder_dir / "ManagedFolderState.java",
            folder_dir / "FolderUpdatePlan.java",
            folder_dir / "FolderMutationPlanner.java",
            folder_dir / "FolderApplyCoordinator.java",
        ]
        for source in sources:
            self.assertTrue(source.is_file(), source)

        with tempfile.TemporaryDirectory() as tmp:
            tmp = Path(tmp)
            harness = tmp / "CoordinatorHarness.java"
            harness.write_text(r'''
import java.util.*;
import org.telegram.teleflow.folders.*;

public class CoordinatorHarness {
    public static void main(String[] args) {
        FolderServerSnapshot current = new FolderServerSnapshot(
            2, "AI", Arrays.asList(10L, 99L), Arrays.asList(99L), Arrays.asList(77L)
        );
        ManagedFolderState managed = new ManagedFolderState(2, "AI", Arrays.asList(10L));
        List<String> events = new ArrayList<>();
        final ManagedFolderState[] stateBox = new ManagedFolderState[] { managed };

        FolderApplyCoordinator.BackupPersistence backup = new FolderApplyCoordinator.BackupPersistence() {
            FolderServerSnapshot value;
            public boolean save(FolderServerSnapshot snapshot) { events.add("backup"); value = snapshot; return true; }
            public FolderServerSnapshot load(int folderId) { return value; }
            public boolean delete(int folderId) { events.add("backup-delete"); value = null; return true; }
        };
        FolderApplyCoordinator.ManagedPersistence state = new FolderApplyCoordinator.ManagedPersistence() {
            public boolean save(ManagedFolderState next) { events.add("managed-save"); stateBox[0] = next; return true; }
            public boolean delete(int folderId) { events.add("managed-delete"); stateBox[0] = null; return true; }
        };
        FolderApplyCoordinator.Remote remote = new FolderApplyCoordinator.Remote() {
            public void apply(FolderUpdatePlan plan, FolderApplyCoordinator.RemoteCallback callback) {
                events.add("remote");
                if (!events.contains("backup")) throw new AssertionError("remote called before backup");
                callback.onResult(true, null);
            }
            public void restore(FolderServerSnapshot snapshot, FolderApplyCoordinator.RemoteCallback callback) {
                events.add("restore");
                callback.onResult(true, null);
            }
        };
        final FolderApplyCoordinator.Result[] result = new FolderApplyCoordinator.Result[1];

        FolderApplyCoordinator.apply(current, managed, Arrays.asList(11L, 11L), backup, state, remote, r -> result[0] = r);
        if (!result[0].isSuccess()) throw new AssertionError(result[0].getCode());
        if (!events.equals(Arrays.asList("backup", "remote", "managed-save"))) throw new AssertionError(events.toString());
        if (!stateBox[0].getManagedDialogIds().equals(Arrays.asList(11L))) throw new AssertionError("managed state was not normalized");

        events.clear();
        FolderApplyCoordinator.Remote failingRemote = new FolderApplyCoordinator.Remote() {
            public void apply(FolderUpdatePlan plan, FolderApplyCoordinator.RemoteCallback callback) {
                events.add("remote-fail");
                callback.onResult(false, "RPC_FAIL");
            }
            public void restore(FolderServerSnapshot snapshot, FolderApplyCoordinator.RemoteCallback callback) {
                throw new AssertionError("restore should not run after failed apply");
            }
        };
        FolderApplyCoordinator.apply(current, managed, Arrays.asList(12L), backup, state, failingRemote, r -> result[0] = r);
        if (result[0].isSuccess() || events.contains("managed-save")) throw new AssertionError("failed RPC persisted managed state");

        events.clear();
        FolderApplyCoordinator.ManagedPersistence brokenState = new FolderApplyCoordinator.ManagedPersistence() {
            public boolean save(ManagedFolderState next) { events.add("managed-save-fail"); return false; }
            public boolean delete(int folderId) { return false; }
        };
        FolderApplyCoordinator.apply(current, managed, Arrays.asList(13L), backup, brokenState, remote, r -> result[0] = r);
        if (result[0].isSuccess() || !events.contains("restore")) throw new AssertionError("state failure did not rollback remote mutation");
    }
}
''')
            classes = tmp / "classes"
            classes.mkdir()
            subprocess.run(["javac", "-d", str(classes), *map(str, sources), str(harness)], check=True)
            subprocess.run(["java", "-cp", str(classes), "CoordinatorHarness"], check=True)


if __name__ == "__main__":
    unittest.main()
