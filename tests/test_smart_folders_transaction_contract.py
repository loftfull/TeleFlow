import unittest
from pathlib import Path


class SmartFoldersTransactionContractTests(unittest.TestCase):
    def test_transaction_wires_snapshot_stores_coordinator_and_gateway(self):
        root = Path(__file__).resolve().parents[1]
        path = root / "overlay/java/org/telegram/teleflow/integration/SmartFoldersTransaction.java"
        self.assertTrue(path.is_file(), path)
        text = path.read_text()
        for token in [
            "TelegramFolderGateway.snapshot",
            "FolderApplyCoordinator.apply",
            "FolderApplyCoordinator.undo",
            "FolderBackupStore",
            "ManagedFolderStateStore",
            "TelegramFolderGateway.applyExisting",
            "TelegramFolderGateway.restoreSnapshot",
        ]:
            self.assertIn(token, text)


if __name__ == "__main__":
    unittest.main()
