/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FolderProvisioningPlanner {
    private FolderProvisioningPlanner() {}

    public static FolderProvisioningPlan plan(
        Map<String, Integer> previewCounts,
        Set<String> alreadyManagedLogicalKeys,
        int currentUserFolderCount,
        int folderLimit
    ) {
        if (currentUserFolderCount < 0 || folderLimit < 0) {
            throw new IllegalArgumentException("folder counts must be non-negative");
        }
        Map<String, Integer> counts = previewCounts == null ? Collections.emptyMap() : previewCounts;
        Set<String> managed = alreadyManagedLogicalKeys == null ? Collections.emptySet() : alreadyManagedLogicalKeys;
        int available = Math.max(0, folderLimit - currentUserFolderCount);
        int remaining = available;
        List<ManagedFolderSpec> create = new ArrayList<>();
        List<ManagedFolderSpec> blocked = new ArrayList<>();

        for (ManagedFolderSpec spec : ManagedFolderCatalog.create()) {
            int count = Math.max(0, counts.getOrDefault(spec.getDisplayName(), 0));
            if (count == 0 || managed.contains(spec.getLogicalKey())) {
                continue;
            }
            if (remaining > 0) {
                create.add(spec);
                remaining--;
            } else {
                blocked.add(spec);
            }
        }
        return new FolderProvisioningPlan(available, create, blocked);
    }
}
