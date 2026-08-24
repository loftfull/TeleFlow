import subprocess
import tempfile
import unittest
from pathlib import Path


class FolderLimitPolicyTests(unittest.TestCase):
    def test_matches_official_telegram_default_and_premium_accounting(self):
        root = Path(__file__).resolve().parents[1]
        source = root / 'overlay/java/org/telegram/teleflow/folders/TelegramFolderLimitPolicy.java'
        self.assertTrue(source.is_file(), source)
        harness = Path(tempfile.mkdtemp()) / 'LimitHarness.java'
        harness.write_text(r'''
import org.telegram.teleflow.folders.TelegramFolderLimitPolicy;

public class LimitHarness {
    public static void main(String[] args) {
        if (TelegramFolderLimitPolicy.currentUserFolderCount(1) != 0) throw new AssertionError("default filter only");
        if (TelegramFolderLimitPolicy.effectiveUserFolderLimit(false, 10, 20) != 10) throw new AssertionError("free limit");
        if (TelegramFolderLimitPolicy.effectiveUserFolderLimit(true, 10, 20) != 19) throw new AssertionError("premium effective limit");
        if (TelegramFolderLimitPolicy.availableSlots(true, 19, 10, 20) != 1) throw new AssertionError("premium one slot");
        if (TelegramFolderLimitPolicy.availableSlots(true, 20, 10, 20) != 0) throw new AssertionError("premium full");
        if (TelegramFolderLimitPolicy.availableSlots(false, 11, 10, 20) != 0) throw new AssertionError("free full");
    }
}
''', encoding='utf-8')
        classes = Path(tempfile.mkdtemp())
        subprocess.run(['javac','-d',str(classes),str(source),str(harness)],check=True)
        subprocess.run(['java','-cp',str(classes),'LimitHarness'],check=True)


if __name__ == '__main__':
    unittest.main()
