/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ManagedFolderStateCodec {
    private static final String VERSION = "v1";
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private ManagedFolderStateCodec() {}

    public static String encode(ManagedFolderState state) {
        if (state == null) {
            throw new IllegalArgumentException("state is required");
        }
        StringBuilder ids = new StringBuilder();
        for (Long id : state.getManagedDialogIds()) {
            if (id == null) { continue; }
            if (ids.length() > 0) { ids.append(','); }
            ids.append(id.longValue());
        }
        return VERSION + "|" + state.getFolderId() + "|" + hex(state.getFolderName()) + "|" + ids;
    }

    public static ManagedFolderState decode(String encoded) {
        if (encoded == null || encoded.trim().isEmpty()) {
            throw new IllegalArgumentException("encoded state is required");
        }
        String[] parts = encoded.split("\\|", -1);
        if (parts.length != 4 || !VERSION.equals(parts[0])) {
            throw new IllegalArgumentException("unsupported managed state format");
        }
        final int folderId;
        try {
            folderId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("invalid folder id", error);
        }
        String folderName = unhex(parts[2]);
        List<Long> ids = new ArrayList<>();
        if (!parts[3].isEmpty()) {
            for (String value : parts[3].split(",")) {
                try {
                    ids.add(Long.parseLong(value));
                } catch (NumberFormatException error) {
                    throw new IllegalArgumentException("invalid managed dialog id", error);
                }
            }
        }
        return new ManagedFolderState(folderId, folderName, Collections.unmodifiableList(ids));
    }

    private static String hex(String value) {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xff;
            out[i * 2] = HEX[b >>> 4];
            out[i * 2 + 1] = HEX[b & 0x0f];
        }
        return new String(out);
    }

    private static String unhex(String value) {
        if ((value.length() & 1) != 0) {
            throw new IllegalArgumentException("invalid hex string length");
        }
        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int high = Character.digit(value.charAt(i * 2), 16);
            int low = Character.digit(value.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("invalid hex string");
            }
            bytes[i] = (byte) ((high << 4) | low);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
