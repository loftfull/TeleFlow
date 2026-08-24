import unittest
from pathlib import Path


class DefaultRulesTests(unittest.TestCase):
    def test_default_rules_classify_russian_and_english_topics(self):
        repo = Path(__file__).resolve().parents[1]
        rules = repo / "overlay/java/org/telegram/teleflow/folders/DefaultFolderRules.java"
        self.assertTrue(rules.is_file())
        text = rules.read_text()
        for expected in ["AI & Tech", "Финансы", "Авто", "Новости", "Покупки", "Обучение", "Боты"]:
            self.assertIn(expected, text)
        for keyword in ["openai", "нейросет", "nasdaq", "инвести", "volvo", "новости", "ozon", "обучение"]:
            self.assertIn(keyword, text.lower())


if __name__ == "__main__":
    unittest.main()
