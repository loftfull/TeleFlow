/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class FolderMutationPlanner {
    private FolderMutationPlanner() {}

    public static FolderUpdatePlan merge(FolderServerSnapshot current, ManagedFolderState managed, List<Long> desiredManagedDialogIds) {
        if (current == null || managed == null) {
            throw new IllegalArgumentException("current and managed state are required");
        }
        if (current.getFolderId() != managed.getFolderId()) {
            throw new IllegalArgumentException("folder id mismatch");
        }

        List<Long> previousManaged = managed.getManagedDialogIds();
        Set<Long> previousManagedSet = new LinkedHashSet<>(previousManaged);
        List<Long> desired = deduplicate(desiredManagedDialogIds);
        Set<Long> desiredSet = new LinkedHashSet<>(desired);

        List<Long> manualIncludes = new ArrayList<>();
        for (Long dialogId : current.getIncludeDialogIds()) {
            if (dialogId != null && !previousManagedSet.contains(dialogId)) {
                manualIncludes.add(dialogId);
            }
        }

        List<Long> mergedIncludes = new ArrayList<>(manualIncludes);
        for (Long dialogId : desired) {
            if (!mergedIncludes.contains(dialogId)) {
                mergedIncludes.add(dialogId);
            }
        }

        List<Long> added = new ArrayList<>();
        for (Long dialogId : desired) {
            if (!previousManagedSet.contains(dialogId)) {
                added.add(dialogId);
            }
        }

        List<Long> removed = new ArrayList<>();
        for (Long dialogId : previousManaged) {
            if (dialogId != null && !desiredSet.contains(dialogId)) {
                removed.add(dialogId);
            }
        }

        return new FolderUpdatePlan(
            current.getFolderId(),
            current.getFolderName(),
            mergedIncludes,
            current.getPinnedDialogIds(),
            current.getExcludeDialogIds(),
            added,
            removed
        );
    }

    private static List<Long> deduplicate(List<Long> values) {
        if (values == null) { return Collections.emptyList(); }
        return new ArrayList<>(new LinkedHashSet<>(values));
    }
}
