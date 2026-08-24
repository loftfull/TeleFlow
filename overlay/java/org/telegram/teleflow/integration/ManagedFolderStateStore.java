/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.integration;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.teleflow.folders.ManagedFolderState;
import org.telegram.teleflow.folders.ManagedFolderStateCodec;

public final class ManagedFolderStateStore {
    private static final String PREFS_PREFIX = "teleflow_managed_folders_";
    private static final String KEY_PREFIX = "folder_";

    private final SharedPreferences preferences;

    public ManagedFolderStateStore(Context context, int account) {
        if (context == null) {
            throw new IllegalArgumentException("context is required");
        }
        if (account < 0) {
            throw new IllegalArgumentException("account must be non-negative");
        }
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_PREFIX + account, Context.MODE_PRIVATE);
    }

    public ManagedFolderState load(int folderId) {
        String encoded = preferences.getString(key(folderId), null);
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        return ManagedFolderStateCodec.decode(encoded);
    }

    public boolean save(ManagedFolderState state) {
        if (state == null) {
            throw new IllegalArgumentException("state is required");
        }
        return preferences.edit().putString(key(state.getFolderId()), ManagedFolderStateCodec.encode(state)).commit();
    }

    public boolean delete(int folderId) {
        return preferences.edit().remove(key(folderId)).commit();
    }

    private static String key(int folderId) {
        if (folderId < 0) {
            throw new IllegalArgumentException("folderId must be non-negative");
        }
        return KEY_PREFIX + folderId;
    }
}
