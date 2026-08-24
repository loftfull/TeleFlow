import unittest
from pathlib import Path


class ManagedStateStoreContractTests(unittest.TestCase):
    def test_store_is_account_scoped_and_uses_versioned_codec(self):
        root = Path(__file__).resolve().parents[1]
        store = root / "overlay/java/org/telegram/teleflow/integration/ManagedFolderStateStore.java"
        self.assertTrue(store.is_file())
        text = store.read_text()
        self.assertIn("teleflow_managed_folders_", text)
        self.assertIn("ManagedFolderStateCodec.encode", text)
        self.assertIn("ManagedFolderStateCodec.decode", text)
        self.assertIn("folder_", text)
        self.assertIn("SharedPreferences", text)
        self.assertNotIn("TELEGRAM_API_HASH", text)


if __name__ == "__main__":
    unittest.main()
