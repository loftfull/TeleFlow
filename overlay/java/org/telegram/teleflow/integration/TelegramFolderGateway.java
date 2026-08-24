/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.integration;

import java.util.ArrayList;
import java.util.Comparator;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.support.LongSparseIntArray;
import org.telegram.teleflow.folders.FolderServerSnapshot;
import org.telegram.teleflow.folders.FolderUpdatePlan;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.FilterCreateActivity;

public final class TelegramFolderGateway {
    private TelegramFolderGateway() {}

    public static FolderServerSnapshot snapshot(MessagesController.DialogFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("filter is required");
        }
        return new FolderServerSnapshot(
            filter.id,
            filter.name,
            new ArrayList<>(filter.alwaysShow),
            orderedPinned(filter.pinnedDialogs),
            new ArrayList<>(filter.neverShow)
        );
    }

    public static void applyExisting(BaseFragment fragment, MessagesController.DialogFilter filter, FolderUpdatePlan plan, Runnable onFinish) {
        if (fragment == null || filter == null || plan == null) {
            throw new IllegalArgumentException("fragment, filter and plan are required");
        }
        if (filter.id != plan.getFolderId()) {
            throw new IllegalArgumentException("filter id does not match update plan");
        }

        ArrayList<Long> include = new ArrayList<>(plan.getIncludeDialogIds());
        ArrayList<Long> exclude = new ArrayList<>(plan.getExcludeDialogIds());
        LongSparseIntArray pinned = new LongSparseIntArray(plan.getPinnedDialogIds().size());
        for (int i = 0; i < plan.getPinnedDialogIds().size(); i++) {
            pinned.put(plan.getPinnedDialogIds().get(i), i);
        }

        // Reuse Telegram's own server + local-storage update path. With progress=true,
        // processAddFilter() runs only after the server callback.
        FilterCreateActivity.saveFilterToServer(
            filter,
            filter.flags,
            filter.name,
            filter.entities,
            filter.title_noanimate,
            filter.color,
            include,
            exclude,
            pinned,
            false,
            false,
            true,
            true,
            true,
            fragment,
            onFinish
        );
    }

    private static ArrayList<Long> orderedPinned(LongSparseIntArray pinned) {
        ArrayList<long[]> pairs = new ArrayList<>();
        if (pinned != null) {
            for (int i = 0; i < pinned.size(); i++) {
                pairs.add(new long[] { pinned.keyAt(i), pinned.valueAt(i) });
            }
        }
        pairs.sort(Comparator.comparingLong(value -> value[1]));
        ArrayList<Long> result = new ArrayList<>(pairs.size());
        for (long[] pair : pairs) {
            result.add(pair[0]);
        }
        return result;
    }
}
