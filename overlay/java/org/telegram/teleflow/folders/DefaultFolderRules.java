/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DefaultFolderRules {
    private DefaultFolderRules() {}

    public static List<FolderRule> create() {
        ArrayList<FolderRule> rules = new ArrayList<>();
        rules.add(new FolderRule("AI & Tech", null, Arrays.asList(
            "openai", "chatgpt", "claude", "gemini", "llm", "artificial intelligence",
            "нейросет", "нейрон", "искусственный интеллект", "ai news", "github", "coding", "developer"
        )));
        rules.add(new FolderRule("Финансы", null, Arrays.asList(
            "nasdaq", "moex", "investment", "investing", "trading", "stock", "market",
            "инвести", "трейдинг", "акци", "рынок", "банк", "finance", "crypto", "крипто"
        )));
        rules.add(new FolderRule("Авто", null, Arrays.asList(
            "volvo", "auto", "automotive", "car", "авто", "машин", "запчаст", "ремонт авто"
        )));
        rules.add(new FolderRule("Новости", ChatDescriptor.PeerType.CHANNEL, Arrays.asList(
            "news", "новости", "рбк", "reuters", "bbc", "cnn", "сми", "breaking"
        )));
        rules.add(new FolderRule("Покупки", null, Arrays.asList(
            "ozon", "wildberries", "aliexpress", "amazon", "marketplace", "магазин", "покупк", "shopping"
        )));
        rules.add(new FolderRule("Обучение", null, Arrays.asList(
            "course", "courses", "education", "learning", "english", "language",
            "курс", "обучение", "образован", "английск", "язык"
        )));
        rules.add(new FolderRule("Боты", ChatDescriptor.PeerType.BOT, Collections.emptyList()));
        return Collections.unmodifiableList(rules);
    }
}
