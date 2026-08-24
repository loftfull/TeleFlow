import unittest
from pathlib import Path


class SmartFoldersApplyUiContractTests(unittest.TestCase):
    def test_ui_requires_confirmation_and_uses_batch_apply_not_direct_gateway(self):
        root = Path(__file__).resolve().parents[1]
        path = root / 'overlay/java/org/telegram/teleflow/ui/TeleFlowSmartFoldersActivity.java'
        self.assertTrue(path.is_file(), path)
        text = path.read_text()
        for token in [
            'TeleFlowApply',
            'AlertDialog.Builder',
            'SmartFoldersBatchApply.apply',
            'setOnClickListener',
            'TeleFlowApplyConfirm',
            'TeleFlowApplySuccess',
            'TeleFlowApplyFailed',
        ]:
            self.assertIn(token, text)
        self.assertNotIn('TelegramFolderGateway.applyExisting', text)
        self.assertNotIn('TL_messages_updateDialogFilter', text)


if __name__ == '__main__':
    unittest.main()
