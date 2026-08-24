import unittest
from pathlib import Path


class FolderRegistryContractTests(unittest.TestCase):
    def test_registry_is_account_scoped_id_based_and_durable(self):
        root = Path(__file__).resolve().parents[1]
        path = root / "overlay/java/org/telegram/teleflow/integration/ManagedFolderRegistryStore.java"
        self.assertTrue(path.is_file(), path)
        text = path.read_text()
        for token in [
            "teleflow_folder_registry_",
            "loadFolderId",
            "saveFolderId",
            ".commit()",
            "normalizeKey",
        ]:
            self.assertIn(token, text)
        self.assertNotIn(".apply();", text)
        self.assertIn("folderId < 2", text)


if __name__ == "__main__":
    unittest.main()
