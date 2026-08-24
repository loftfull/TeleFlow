import unittest
from pathlib import Path


class NativeGatewayContractTests(unittest.TestCase):
    def test_gateway_reuses_official_telegram_filter_save_path(self):
        root = Path(__file__).resolve().parents[1]
        gateway = root / "overlay/java/org/telegram/teleflow/integration/TelegramFolderGateway.java"
        self.assertTrue(gateway.is_file())
        text = gateway.read_text()
        self.assertIn("FilterCreateActivity.saveFilterToServer", text)
        self.assertIn("FolderUpdatePlan", text)
        self.assertIn("LongSparseIntArray", text)
        self.assertNotIn("new TLRPC.TL_messages_updateDialogFilter", text)


if __name__ == "__main__":
    unittest.main()
