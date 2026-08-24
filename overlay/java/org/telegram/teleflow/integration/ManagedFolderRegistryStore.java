/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.integration;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public final class ManagedFolderRegistryStore {
    private static final String PREFS_PREFIX = "teleflow_folder_registry_";
    private static final String KEY_PREFIX = "logical_";

    private final SharedPreferences preferences;

    public ManagedFolderRegistryStore(Context context, int account) {
        if (context == null) {
            throw new IllegalArgumentException("context is required");
        }
        if (account < 0) {
            throw new IllegalArgumentException("account must be non-negative");
        }
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_PREFIX + account, Context.MODE_PRIVATE);
    }

    public Integer loadFolderId(String logicalKey) {
        String key = key(logicalKey);
        if (!preferences.contains(key)) {
            return null;
        }
        int value = preferences.getInt(key, -1);
        return value >= 2 ? value : null;
    }

    public boolean saveFolderId(String logicalKey, int folderId) {
        if (folderId < 2) {
            throw new IllegalArgumentException("managed folderId must be >= 2");
        }
        return preferences.edit().putInt(key(logicalKey), folderId).commit();
    }

    public boolean delete(String logicalKey) {
        return preferences.edit().remove(key(logicalKey)).commit();
    }

    private static String key(String logicalKey) {
        return KEY_PREFIX + normalizeKey(logicalKey);
    }

    static String normalizeKey(String logicalKey) {
        if (logicalKey == null) {
            throw new IllegalArgumentException("logicalKey is required");
        }
        String normalized = logicalKey.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9_-]{0,63}")) {
            throw new IllegalArgumentException("logicalKey must use stable ASCII letters, digits, '_' or '-'");
        }
        return normalized;
    }
}
