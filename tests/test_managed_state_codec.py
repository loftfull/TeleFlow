import subprocess
import tempfile
import unittest
from pathlib import Path


class ManagedStateCodecTests(unittest.TestCase):
    def test_round_trips_versioned_state_with_unicode_and_stable_ids(self):
        repo = Path(__file__).resolve().parents[1]
        folder_dir = repo / "overlay/java/org/telegram/teleflow/folders"
        sources = [
            folder_dir / "ManagedFolderState.java",
            folder_dir / "ManagedFolderStateCodec.java",
        ]
        work = Path(tempfile.mkdtemp())
        harness = work / "ManagedStateCodecHarness.java"
        harness.write_text(r'''
import java.util.Arrays;
import org.telegram.teleflow.folders.ManagedFolderState;
import org.telegram.teleflow.folders.ManagedFolderStateCodec;

public class ManagedStateCodecHarness {
    public static void main(String[] args) {
        ManagedFolderState original = new ManagedFolderState(7, "Финансы / AI", Arrays.asList(10L, -20L, 30L));
        String encoded = ManagedFolderStateCodec.encode(original);
        if (!encoded.startsWith("v1|7|")) throw new AssertionError("missing stable version header: " + encoded);
        ManagedFolderState decoded = ManagedFolderStateCodec.decode(encoded);
        if (decoded.getFolderId() != 7) throw new AssertionError("folder id changed");
        if (!"Финансы / AI".equals(decoded.getFolderName())) throw new AssertionError("unicode name changed");
        if (!decoded.getManagedDialogIds().equals(Arrays.asList(10L, -20L, 30L))) throw new AssertionError("ids changed");

        boolean rejected = false;
        try {
            ManagedFolderStateCodec.decode("v2|7|00|");
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        if (!rejected) throw new AssertionError("unknown version accepted");
    }
}
''')
        classes = work / "classes"
        classes.mkdir()
        subprocess.run(["javac", "-d", str(classes), *map(str, sources), str(harness)], check=True)
        subprocess.run(["java", "-cp", str(classes), "ManagedStateCodecHarness"], check=True)


if __name__ == "__main__":
    unittest.main()
