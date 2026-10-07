/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FolderPreviewPlanner {
    private FolderPreviewPlanner() {}

    public static FolderPreviewPlan plan(List<ChatDescriptor> chats, FolderRuleEngine engine, String reviewFolder) {
        if (engine == null) {
            throw new IllegalArgumentException("engine is required");
        }
        List<ChatDescriptor> safeChats = chats == null ? Collections.emptyList() : chats;
        String review = reviewFolder == null || reviewFolder.trim().isEmpty() ? "Review" : reviewFolder.trim();
        Map<String, List<FolderAssignment>> grouped = new LinkedHashMap<>();
        int reviewCount = 0;
        for (ChatDescriptor chat : safeChats) {
            if (chat == null) { continue; }
            ClassificationDecision decision = engine.classify(chat);
            FolderAssignment assignment = new FolderAssignment(chat, decision);
            grouped.computeIfAbsent(decision.getFolder(), ignored -> new ArrayList<>()).add(assignment);
            if (review.equals(decision.getFolder())) {
                reviewCount++;
            }
        }
        int total = 0;
        for (List<FolderAssignment> values : grouped.values()) {
            total += values.size();
        }
        return new FolderPreviewPlan(grouped, total, reviewCount);
    }
}
