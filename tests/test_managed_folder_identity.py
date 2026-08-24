import subprocess
import tempfile
import unittest
from pathlib import Path


class ManagedFolderIdentityTests(unittest.TestCase):
    def test_identity_guard_rejects_reused_or_renamed_folder_ids(self):
        root = Path(__file__).resolve().parents[1]
        folders = root / "overlay/java/org/telegram/teleflow/folders"
        expected = [
            folders / "ManagedFolderRegistration.java",
            folders / "ManagedFolderResolution.java",
            folders / "ManagedFolderIdentityGuard.java",
        ]
        for path in expected:
            self.assertTrue(path.is_file(), path)

        harness = Path(tempfile.mkdtemp()) / "IdentityHarness.java"
        harness.write_text(r'''
import java.util.*;
import org.telegram.teleflow.folders.*;

public class IdentityHarness {
    public static void main(String[] args) {
        ManagedFolderRegistration reg = new ManagedFolderRegistration("finance", 7, "TF Финансы");
        Map<Integer, String> live = new HashMap<>();
        live.put(7, "TF Финансы");
        ManagedFolderResolution ok = ManagedFolderIdentityGuard.resolve(reg, live);
        if (ok.getStatus() != ManagedFolderResolution.Status.ACTIVE) throw new AssertionError("active");

        live.put(7, "Личное");
        ManagedFolderResolution mismatch = ManagedFolderIdentityGuard.resolve(reg, live);
        if (mismatch.getStatus() != ManagedFolderResolution.Status.IDENTITY_MISMATCH) throw new AssertionError("mismatch");

        live.clear();
        ManagedFolderResolution missing = ManagedFolderIdentityGuard.resolve(reg, live);
        if (missing.getStatus() != ManagedFolderResolution.Status.MISSING) throw new AssertionError("missing");

        ManagedFolderResolution unregistered = ManagedFolderIdentityGuard.resolve(null, live);
        if (unregistered.getStatus() != ManagedFolderResolution.Status.UNREGISTERED) throw new AssertionError("unregistered");
    }
}
''', encoding='utf-8')
        classes = Path(tempfile.mkdtemp())
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(classes), *map(str, expected), str(harness)], check=True)
        subprocess.run(["java", "-cp", str(classes), "IdentityHarness"], check=True)


if __name__ == '__main__':
    unittest.main()
