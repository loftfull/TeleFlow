import unittest
from pathlib import Path


class SmartFoldersUiContractTests(unittest.TestCase):
    def test_screen_runs_metadata_only_preview_without_applying_changes(self):
        root = Path(__file__).resolve().parents[1]
        screen = root / "overlay/java/org/telegram/teleflow/ui/TeleFlowSmartFoldersActivity.java"
        text = screen.read_text()
        self.assertIn("TelegramDialogReader.readLoadedDialogs", text)
        self.assertIn("DefaultFolderRules.create", text)
        self.assertIn("FolderPreviewPlanner.plan", text)
        self.assertIn("getReviewCount", text)
        self.assertNotIn("TelegramFolderGateway.applyExisting", text)
        self.assertNotIn("saveFilterToServer", text)


if __name__ == "__main__":
    unittest.main()
