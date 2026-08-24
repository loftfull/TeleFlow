import unittest
from pathlib import Path


class ReferenceUiContractTests(unittest.TestCase):
    def setUp(self):
        self.root = Path(__file__).resolve().parents[1]
        self.ui_root = self.root / "overlay/java/org/telegram/teleflow/ui"

    def test_shared_reference_palette_and_primitives_exist(self):
        helper = self.ui_root / "TeleFlowUi.java"
        self.assertTrue(helper.is_file(), helper)
        text = helper.read_text()
        for token in [
            '#09111F',
            '#111C2C',
            '#18263A',
            '#4C7DFF',
            '#8B5CF6',
            '#2DD4BF',
            'GradientDrawable',
            'screenBackground',
            'cardBackground',
            'accentBackground',
        ]:
            self.assertIn(token, text)

    def test_existing_skeleton_screens_use_shared_reference_layer(self):
        for filename in [
            "TeleFlowHubActivity.java",
            "TeleFlowSmartFoldersActivity.java",
            "TeleFlowDriveActivity.java",
        ]:
            text = (self.ui_root / filename).read_text()
            self.assertIn("TeleFlowUi.", text, filename)
            self.assertNotIn("Theme.key_windowBackgroundGray", text, filename)

    def test_hub_does_not_add_post_skeleton_features(self):
        hub = (self.ui_root / "TeleFlowHubActivity.java").read_text()
        for forbidden in [
            "TeleFlowPriorityInboxActivity",
            "TeleFlowAutomationActivity",
            "TeleFlowAiActivity",
        ]:
            self.assertNotIn(forbidden, hub)


if __name__ == "__main__":
    unittest.main()
