import unittest
from pathlib import Path


class NativeGatewayContractTests(unittest.TestCase):
    def test_gateway_updates_local_state_only_after_rpc_success(self):
        root = Path(__file__).resolve().parents[1]
        gateway = root / "overlay/java/org/telegram/teleflow/integration/TelegramFolderGateway.java"
        self.assertTrue(gateway.is_file())
        text = gateway.read_text()

        # The upstream helper currently updates local state after the callback even when
        # Telegram returns an RPC error, so TeleFlow must own the error-aware transaction.
        self.assertNotIn("FilterCreateActivity.saveFilterToServer", text)
        self.assertIn("new TLRPC.TL_messages_updateDialogFilter", text)
        self.assertIn("ResultCallback", text)
        self.assertIn("if (error != null)", text)
        self.assertIn("callback.onResult(false", text)
        self.assertIn("callback.onResult(true", text)
        self.assertIn("restoreSnapshot", text)
        self.assertIn("filter.pinnedDialogs.clear()", text)


if __name__ == "__main__":
    unittest.main()
