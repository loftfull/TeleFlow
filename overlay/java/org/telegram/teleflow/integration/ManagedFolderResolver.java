/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.integration;

import android.content.Context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.teleflow.folders.ManagedFolderCatalog;
import org.telegram.teleflow.folders.ManagedFolderIdentityGuard;
import org.telegram.teleflow.folders.ManagedFolderRegistration;
import org.telegram.teleflow.folders.ManagedFolderResolution;
import org.telegram.teleflow.folders.ManagedFolderSpec;
import org.telegram.teleflow.folders.TelegramFolderLimitPolicy;

public final class ManagedFolderResolver {
    private ManagedFolderResolver() {}

    public static ManagedFolderInventory resolve(Context context, int account) {
        if (context == null) throw new IllegalArgumentException("context is required");
        if (account < 0) throw new IllegalArgumentException("account must be non-negative");

        MessagesController controller = MessagesController.getInstance(account);
        ManagedFolderRegistryStore registry = new ManagedFolderRegistryStore(context, account);
        List<MessagesController.DialogFilter> liveFilters = controller.getDialogFilters();
        Map<Integer, String> liveNames = new LinkedHashMap<>();
        for (MessagesController.DialogFilter filter : liveFilters) {
            if (filter != null) liveNames.put(filter.id, filter.name);
        }

        Map<String, Integer> active = new LinkedHashMap<>();
        Map<String, ManagedFolderResolution.Status> statuses = new LinkedHashMap<>();
        for (ManagedFolderSpec spec : ManagedFolderCatalog.create()) {
            ManagedFolderRegistration registration = registry.loadRegistration(spec.getLogicalKey());
            ManagedFolderResolution resolution = ManagedFolderIdentityGuard.resolve(registration, liveNames);
            statuses.put(spec.getLogicalKey(), resolution.getStatus());
            if (resolution.isActive()) {
                active.put(spec.getLogicalKey(), registration.getFolderId());
            } else if (resolution.getStatus() == ManagedFolderResolution.Status.IDENTITY_MISMATCH) {
                // Fail closed: never adopt a live folder merely because its id or visible title looks familiar.
            }
        }

        boolean premium = UserConfig.getInstance(account).isPremium();
        int currentUserFolderCount = TelegramFolderLimitPolicy.currentUserFolderCount(liveFilters.size());
        int folderLimit = TelegramFolderLimitPolicy.effectiveUserFolderLimit(
            premium, controller.dialogFiltersLimitDefault, controller.dialogFiltersLimitPremium
        );
        return new ManagedFolderInventory(active, statuses, currentUserFolderCount, folderLimit);
    }
}
