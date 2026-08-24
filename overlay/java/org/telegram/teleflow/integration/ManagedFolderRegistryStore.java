/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.integration;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

import org.telegram.teleflow.folders.ManagedFolderRegistration;

public final class ManagedFolderRegistryStore {
    private static final String PREFS_PREFIX = "teleflow_folder_registry_";
    private static final String KEY_PREFIX = "logical_";
    private static final String KEY_ID_SUFFIX = "_id";
    private static final String KEY_NAME_SUFFIX = "_name";

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

    public ManagedFolderRegistration loadRegistration(String logicalKey) {
        String prefix = key(logicalKey);
        if (!preferences.contains(prefix + KEY_ID_SUFFIX)) {
            return null;
        }
        int folderId = preferences.getInt(prefix + KEY_ID_SUFFIX, -1);
        String serverName = preferences.getString(prefix + KEY_NAME_SUFFIX, null);
        if (folderId < 2 || serverName == null || serverName.trim().isEmpty()) {
            return null;
        }
        return new ManagedFolderRegistration(normalizeKey(logicalKey), folderId, serverName);
    }

    public boolean saveRegistration(ManagedFolderRegistration registration) {
        if (registration == null) {
            throw new IllegalArgumentException("registration is required");
        }
        String prefix = key(registration.getLogicalKey());
        return preferences.edit()
            .putInt(prefix + KEY_ID_SUFFIX, registration.getFolderId())
            .putString(prefix + KEY_NAME_SUFFIX, registration.getExpectedServerName())
            .commit();
    }

    public Integer loadFolderId(String logicalKey) {
        ManagedFolderRegistration registration = loadRegistration(logicalKey);
        return registration == null ? null : registration.getFolderId();
    }

    public boolean saveFolderId(String logicalKey, int folderId) {
        throw new UnsupportedOperationException("Use saveRegistration() so folder identity is persisted with the id");
    }

    public boolean delete(String logicalKey) {
        String prefix = key(logicalKey);
        return preferences.edit()
            .remove(prefix + KEY_ID_SUFFIX)
            .remove(prefix + KEY_NAME_SUFFIX)
            .commit();
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
