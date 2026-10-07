import subprocess
import tempfile
import unittest
from pathlib import Path


class FolderMutationTests(unittest.TestCase):
    def test_preserves_manual_membership_and_calculates_delta(self):
        repo = Path(__file__).resolve().parents[1]
        folder_dir = repo / "overlay/java/org/telegram/teleflow/folders"
        sources = [
            folder_dir / "ManagedFolderState.java",
            folder_dir / "FolderServerSnapshot.java",
            folder_dir / "FolderUpdatePlan.java",
            folder_dir / "FolderMutationPlanner.java",
        ]
        work = Path(tempfile.mkdtemp())
        harness = work / "FolderMutationHarness.java"
        harness.write_text(r'''
import java.util.Arrays;
import org.telegram.teleflow.folders.ManagedFolderState;
import org.telegram.teleflow.folders.FolderServerSnapshot;
import org.telegram.teleflow.folders.FolderUpdatePlan;
import org.telegram.teleflow.folders.FolderMutationPlanner;

public class FolderMutationHarness {
    public static void main(String[] args) {
        ManagedFolderState managed = new ManagedFolderState(7, "Finance", Arrays.asList(1L, 2L));
        FolderServerSnapshot current = new FolderServerSnapshot(
            7,
            "Finance",
            Arrays.asList(100L, 1L, 2L),
            Arrays.asList(100L),
            Arrays.asList(999L)
        );
        FolderUpdatePlan update = FolderMutationPlanner.merge(current, managed, Arrays.asList(2L, 3L));
        if (!update.getIncludeDialogIds().equals(Arrays.asList(100L, 2L, 3L))) throw new AssertionError("manual includes lost");
        if (!update.getPinnedDialogIds().equals(Arrays.asList(100L))) throw new AssertionError("pinned changed");
        if (!update.getExcludeDialogIds().equals(Arrays.asList(999L))) throw new AssertionError("excluded changed");
        if (!update.getAddedDialogIds().equals(Arrays.asList(3L))) throw new AssertionError("added delta wrong");
        if (!update.getRemovedDialogIds().equals(Arrays.asList(1L))) throw new AssertionError("removed delta wrong");
    }
}
''')
        classes = work / "classes"
        classes.mkdir()
        subprocess.run(["javac", "-d", str(classes), *map(str, sources), str(harness)], check=True)
        subprocess.run(["java", "-cp", str(classes), "FolderMutationHarness"], check=True)


if __name__ == "__main__":
    unittest.main()
