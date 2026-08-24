import unittest
from pathlib import Path


class ManagedBackupStoreContractTests(unittest.TestCase):
    def test_store_is_account_scoped_durable_and_uses_managed_codec(self):
        path = Path(__file__).resolve().parents[1] / "overlay/java/org/telegram/teleflow/integration/ManagedFolderStateBackupStore.java"
        self.assertTrue(path.is_file(), path)
        text = path.read_text()
        for token in [
            "teleflow_managed_backups_",
            "ManagedFolderStateCodec.encode",
            "ManagedFolderStateCodec.decode",
            ".commit()",
            "public boolean save",
            "public ManagedFolderState load",
        ]:
            self.assertIn(token, text)
        self.assertNotIn(".apply();", text)


if __name__ == "__main__":
    unittest.main()
