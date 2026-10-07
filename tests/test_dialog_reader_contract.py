import unittest
from pathlib import Path


class DialogReaderContractTests(unittest.TestCase):
    def test_reader_uses_loaded_dialog_metadata_only(self):
        root = Path(__file__).resolve().parents[1]
        reader = root / "overlay/java/org/telegram/teleflow/integration/TelegramDialogReader.java"
        self.assertTrue(reader.is_file())
        text = reader.read_text()
        self.assertIn("dialogs_dict", text)
        self.assertIn("getUser", text)
        self.assertIn("getChat", text)
        self.assertIn("ChatDescriptor", text)
        self.assertNotIn("MessageObject", text)
        self.assertNotIn("messageOwner", text)
        self.assertNotIn("getMessages", text)


if __name__ == "__main__":
    unittest.main()
