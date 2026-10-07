/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.integration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.telegram.teleflow.folders.ManagedFolderResolution;

public final class ManagedFolderInventory {
    private final Map<String, Integer> activeFolderIds;
    private final Map<String, ManagedFolderResolution.Status> statuses;
    private final int currentUserFolderCount;
    private final int folderLimit;

    public ManagedFolderInventory(
        Map<String, Integer> activeFolderIds,
        Map<String, ManagedFolderResolution.Status> statuses,
        int currentUserFolderCount,
        int folderLimit
    ) {
        this.activeFolderIds = Collections.unmodifiableMap(new LinkedHashMap<>(activeFolderIds));
        this.statuses = Collections.unmodifiableMap(new LinkedHashMap<>(statuses));
        this.currentUserFolderCount = Math.max(0, currentUserFolderCount);
        this.folderLimit = Math.max(0, folderLimit);
    }

    public Integer getActiveFolderId(String logicalKey) { return activeFolderIds.get(logicalKey); }
    public Set<String> getActiveLogicalKeys() { return Collections.unmodifiableSet(new LinkedHashSet<>(activeFolderIds.keySet())); }
    public Map<String, ManagedFolderResolution.Status> getStatuses() { return statuses; }
    public int getCurrentUserFolderCount() { return currentUserFolderCount; }
    public int getFolderLimit() { return folderLimit; }
    public int getAvailableSlots() { return Math.max(0, folderLimit - currentUserFolderCount); }
}
