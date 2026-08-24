import unittest
from pathlib import Path


class SmartFoldersApplyUiContractTests(unittest.TestCase):
    def test_ui_is_read_only_and_has_no_mutation_call_path(self):
        root = Path(__file__).resolve().parents[1]
        path = root / 'overlay/java/org/telegram/teleflow/ui/TeleFlowSmartFoldersActivity.java'
        self.assertTrue(path.is_file(), path)
        text = path.read_text(encoding='utf-8')
        for token in [
            'TelegramDialogReader.readLoadedDialogs',
            'FolderPreviewPlanner.plan',
            'TeleFlowSmartFoldersStatus',
        ]:
            self.assertIn(token, text)
        for forbidden in [
            'SmartFoldersBatchApply',
            'showApplyConfirmation',
            'applyPreview',
            'TeleFlowApply',
            'AlertDialog.Builder',
            'setOnClickListener',
        ]:
            self.assertNotIn(forbidden, text)

    def test_user_facing_copy_describes_read_only_preview(self):
        root = Path(__file__).resolve().parents[1]
        english = (root / 'overlay/values/strings.xml.fragment').read_text(encoding='utf-8')
        russian = (root / 'overlay/values-ru/strings.xml.fragment').read_text(encoding='utf-8')
        self.assertIn(
            '<string name="TeleFlowSmartFoldersInfo">Read-only classification preview</string>',
            english,
        )
        self.assertIn(
            '<string name="TeleFlowSmartFoldersInfo">Предпросмотр автоматической сортировки без изменения папок</string>',
            russian,
        )


if __name__ == '__main__':
    unittest.main()
