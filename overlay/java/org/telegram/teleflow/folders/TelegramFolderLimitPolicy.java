/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

public final class TelegramFolderLimitPolicy {
    private TelegramFolderLimitPolicy() {}

    public static int currentUserFolderCount(int totalDialogFilters) {
        return Math.max(0, totalDialogFilters - 1);
    }

    public static int effectiveUserFolderLimit(boolean premium, int defaultLimit, int premiumLimit) {
        int safeDefault = Math.max(0, defaultLimit);
        int safePremium = Math.max(0, premiumLimit);
        return premium ? Math.max(0, safePremium - 1) : safeDefault;
    }

    public static int availableSlots(boolean premium, int totalDialogFilters, int defaultLimit, int premiumLimit) {
        int current = currentUserFolderCount(totalDialogFilters);
        int limit = effectiveUserFolderLimit(premium, defaultLimit, premiumLimit);
        return Math.max(0, limit - current);
    }
}
