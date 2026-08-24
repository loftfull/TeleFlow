/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.integration;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.teleflow.folders.FolderBackupCodec;
import org.telegram.teleflow.folders.FolderServerSnapshot;

public final class FolderBackupStore {
    private static final String PREFS_PREFIX = "teleflow_folder_backups_";
    private static final String KEY_PREFIX = "folder_";

    private final SharedPreferences preferences;

    public FolderBackupStore(Context context, int account) {
        if (context == null) {
            throw new IllegalArgumentException("context is required");
        }
        if (account < 0) {
            throw new IllegalArgumentException("account must be non-negative");
        }
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_PREFIX + account, Context.MODE_PRIVATE);
    }

    public FolderServerSnapshot load(int folderId) {
        String encoded = preferences.getString(key(folderId), null);
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        return FolderBackupCodec.decode(encoded);
    }

    public void save(FolderServerSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot is required");
        }
        preferences.edit().putString(key(snapshot.getFolderId()), FolderBackupCodec.encode(snapshot)).apply();
    }

    public void delete(int folderId) {
        preferences.edit().remove(key(folderId)).apply();
    }

    private static String key(int folderId) {
        if (folderId < 0) {
            throw new IllegalArgumentException("folderId must be non-negative");
        }
        return KEY_PREFIX + folderId;
    }
}
