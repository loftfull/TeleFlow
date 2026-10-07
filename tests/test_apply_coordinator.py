import subprocess
import tempfile
import unittest
from pathlib import Path


class ApplyCoordinatorTests(unittest.TestCase):
    def test_apply_and_undo_preserve_previous_managed_state(self):
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
        ManagedFolderState previous = new ManagedFolderState(2, "AI", Arrays.asList(10L));
        List<String> events = new ArrayList<>();
        final ManagedFolderState[] liveState = new ManagedFolderState[] { previous };

        FolderApplyCoordinator.BackupPersistence serverBackup = new FolderApplyCoordinator.BackupPersistence() {
            FolderServerSnapshot value;
            public boolean save(FolderServerSnapshot snapshot) { events.add("server-backup"); value = snapshot; return true; }
            public FolderServerSnapshot load(int folderId) { return value; }
            public boolean delete(int folderId) { events.add("server-backup-delete"); value = null; return true; }
        };
        FolderApplyCoordinator.ManagedBackupPersistence managedBackup = new FolderApplyCoordinator.ManagedBackupPersistence() {
            ManagedFolderState value;
            public boolean save(ManagedFolderState state) { events.add("managed-backup"); value = state; return true; }
            public ManagedFolderState load(int folderId) { return value; }
            public boolean delete(int folderId) { events.add("managed-backup-delete"); value = null; return true; }
        };
        FolderApplyCoordinator.ManagedPersistence state = new FolderApplyCoordinator.ManagedPersistence() {
            public ManagedFolderState load(int folderId) { return liveState[0]; }
            public boolean save(ManagedFolderState next) { events.add("managed-save"); liveState[0] = next; return true; }
            public boolean delete(int folderId) { liveState[0] = null; return true; }
        };
        FolderApplyCoordinator.Remote remote = new FolderApplyCoordinator.Remote() {
            public void apply(FolderUpdatePlan plan, FolderApplyCoordinator.RemoteCallback callback) {
                events.add("remote");
                if (!events.contains("server-backup") || !events.contains("managed-backup")) {
                    throw new AssertionError("remote called before durable backups");
                }
                callback.onResult(true, null);
            }
            public void restore(FolderServerSnapshot snapshot, FolderApplyCoordinator.RemoteCallback callback) {
                events.add("restore");
                callback.onResult(true, null);
            }
        };
        final FolderApplyCoordinator.Result[] result = new FolderApplyCoordinator.Result[1];

        FolderApplyCoordinator.apply(current, previous, Arrays.asList(11L, 11L), serverBackup, managedBackup, state, remote, r -> result[0] = r);
        if (!result[0].isSuccess()) throw new AssertionError(result[0].getCode());
        if (!liveState[0].getManagedDialogIds().equals(Arrays.asList(11L))) throw new AssertionError("managed state was not normalized");

        FolderServerSnapshot changed = new FolderServerSnapshot(
            2, "AI", Arrays.asList(11L, 99L), Arrays.asList(99L), Arrays.asList(77L)
        );
        FolderApplyCoordinator.undo(changed, 2, serverBackup, managedBackup, state, remote, r -> result[0] = r);
        if (!result[0].isSuccess()) throw new AssertionError(result[0].getCode());
        if (!liveState[0].getManagedDialogIds().equals(Arrays.asList(10L))) throw new AssertionError("previous managed state was not restored");

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
        FolderApplyCoordinator.apply(current, previous, Arrays.asList(12L), serverBackup, managedBackup, state, failingRemote, r -> result[0] = r);
        if (result[0].isSuccess()) throw new AssertionError("failed RPC reported success");
    }
}
''')
            classes = tmp / "classes"
            classes.mkdir()
            subprocess.run(["javac", "-d", str(classes), *map(str, sources), str(harness)], check=True)
            subprocess.run(["java", "-cp", str(classes), "CoordinatorHarness"], check=True)


if __name__ == "__main__":
    unittest.main()
