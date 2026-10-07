import unittest
from pathlib import Path


class FolderRegistryRegistrationContractTests(unittest.TestCase):
    def test_registry_persists_id_and_expected_name_atomically(self):
        root = Path(__file__).resolve().parents[1]
        path = root / "overlay/java/org/telegram/teleflow/integration/ManagedFolderRegistryStore.java"
        text = path.read_text()
        for token in [
            "ManagedFolderRegistration",
            "loadRegistration",
            "saveRegistration",
            "KEY_ID_SUFFIX",
            "KEY_NAME_SUFFIX",
            ".commit()",
        ]:
            self.assertIn(token, text)
        self.assertNotIn(".apply();", text)


if __name__ == '__main__':
    unittest.main()
