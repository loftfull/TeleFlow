import subprocess
import tempfile
import unittest
from pathlib import Path


class FolderBackupCodecTests(unittest.TestCase):
    def test_round_trips_server_snapshot_and_rejects_unknown_version(self):
        repo = Path(__file__).resolve().parents[1]
        folder_dir = repo / "overlay/java/org/telegram/teleflow/folders"
        sources = [
            folder_dir / "FolderServerSnapshot.java",
            folder_dir / "FolderBackupCodec.java",
        ]
        work = Path(tempfile.mkdtemp())
        harness = work / "FolderBackupCodecHarness.java"
        harness.write_text(r'''
import java.util.Arrays;
import org.telegram.teleflow.folders.FolderServerSnapshot;
import org.telegram.teleflow.folders.FolderBackupCodec;

public class FolderBackupCodecHarness {
    public static void main(String[] args) {
        FolderServerSnapshot original = new FolderServerSnapshot(
            9, "Работа / VIP", Arrays.asList(1L, -2L), Arrays.asList(-2L), Arrays.asList(77L)
        );
        String encoded = FolderBackupCodec.encode(original);
        if (!encoded.startsWith("v1|9|")) throw new AssertionError("version missing");
        FolderServerSnapshot decoded = FolderBackupCodec.decode(encoded);
        if (decoded.getFolderId() != 9) throw new AssertionError("id changed");
        if (!"Работа / VIP".equals(decoded.getFolderName())) throw new AssertionError("name changed");
        if (!decoded.getIncludeDialogIds().equals(Arrays.asList(1L, -2L))) throw new AssertionError("include changed");
        if (!decoded.getPinnedDialogIds().equals(Arrays.asList(-2L))) throw new AssertionError("pinned changed");
        if (!decoded.getExcludeDialogIds().equals(Arrays.asList(77L))) throw new AssertionError("exclude changed");
        boolean rejected = false;
        try { FolderBackupCodec.decode("v9|9|00|||"); } catch (IllegalArgumentException expected) { rejected = true; }
        if (!rejected) throw new AssertionError("unknown version accepted");
    }
}
''')
        classes = work / "classes"
        classes.mkdir()
        subprocess.run(["javac", "-d", str(classes), *map(str, sources), str(harness)], check=True)
        subprocess.run(["java", "-cp", str(classes), "FolderBackupCodecHarness"], check=True)


if __name__ == "__main__":
    unittest.main()
