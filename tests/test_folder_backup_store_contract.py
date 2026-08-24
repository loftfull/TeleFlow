import unittest
from pathlib import Path


class FolderBackupStoreContractTests(unittest.TestCase):
    def test_backup_store_is_account_scoped_and_uses_backup_codec(self):
        root = Path(__file__).resolve().parents[1]
        store = root / "overlay/java/org/telegram/teleflow/integration/FolderBackupStore.java"
        self.assertTrue(store.is_file())
        text = store.read_text()
        self.assertIn("teleflow_folder_backups_", text)
        self.assertIn("FolderBackupCodec.encode", text)
        self.assertIn("FolderBackupCodec.decode", text)
        self.assertIn("folder_", text)
        self.assertIn("SharedPreferences", text)


if __name__ == "__main__":
    unittest.main()
