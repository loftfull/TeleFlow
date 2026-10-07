/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FolderBackupCodec {
    private static final String VERSION = "v1";
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private FolderBackupCodec() {}

    public static String encode(FolderServerSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot is required");
        }
        return VERSION + "|" + snapshot.getFolderId() + "|" + hex(snapshot.getFolderName()) + "|"
            + ids(snapshot.getIncludeDialogIds()) + "|" + ids(snapshot.getPinnedDialogIds()) + "|" + ids(snapshot.getExcludeDialogIds());
    }

    public static FolderServerSnapshot decode(String encoded) {
        if (encoded == null || encoded.trim().isEmpty()) {
            throw new IllegalArgumentException("encoded backup is required");
        }
        String[] parts = encoded.split("\\|", -1);
        if (parts.length != 6 || !VERSION.equals(parts[0])) {
            throw new IllegalArgumentException("unsupported folder backup format");
        }
        final int folderId;
        try {
            folderId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("invalid folder id", error);
        }
        return new FolderServerSnapshot(
            folderId,
            unhex(parts[2]),
            parseIds(parts[3]),
            parseIds(parts[4]),
            parseIds(parts[5])
        );
    }

    private static String ids(List<Long> values) {
        StringBuilder out = new StringBuilder();
        if (values != null) {
            for (Long value : values) {
                if (value == null) { continue; }
                if (out.length() > 0) { out.append(','); }
                out.append(value.longValue());
            }
        }
        return out.toString();
    }

    private static List<Long> parseIds(String values) {
        if (values.isEmpty()) { return Collections.emptyList(); }
        ArrayList<Long> result = new ArrayList<>();
        for (String value : values.split(",")) {
            try {
                result.add(Long.parseLong(value));
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("invalid dialog id", error);
            }
        }
        return Collections.unmodifiableList(result);
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
