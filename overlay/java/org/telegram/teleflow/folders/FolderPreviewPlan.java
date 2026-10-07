/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FolderPreviewPlan {
    private final Map<String, List<FolderAssignment>> assignments;
    private final int totalChats;
    private final int reviewCount;

    FolderPreviewPlan(Map<String, List<FolderAssignment>> assignments, int totalChats, int reviewCount) {
        LinkedHashMap<String, List<FolderAssignment>> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, List<FolderAssignment>> entry : assignments.entrySet()) {
            snapshot.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        this.assignments = Collections.unmodifiableMap(snapshot);
        this.totalChats = totalChats;
        this.reviewCount = reviewCount;
    }

    public int getTotalChats() { return totalChats; }
    public int getReviewCount() { return reviewCount; }

    public int getFolderCount(String folder) {
        return getAssignments(folder).size();
    }

    public List<FolderAssignment> getAssignments(String folder) {
        List<FolderAssignment> values = assignments.get(folder);
        return values == null ? Collections.emptyList() : values;
    }

    public Map<String, List<FolderAssignment>> getFolders() { return assignments; }
}
