/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.drive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DriveIndex {
    private final Map<String, List<DriveObject>> byDirectory;
    private final long totalBytes;
    private final int fileCount;

    private DriveIndex(Map<String, List<DriveObject>> byDirectory, long totalBytes, int fileCount) {
        LinkedHashMap<String, List<DriveObject>> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, List<DriveObject>> entry : byDirectory.entrySet()) {
            snapshot.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        this.byDirectory = Collections.unmodifiableMap(snapshot);
        this.totalBytes = totalBytes;
        this.fileCount = fileCount;
    }

    public static DriveIndex from(List<DriveObject> objects) {
        List<DriveObject> safe = objects == null ? Collections.emptyList() : objects;
        Map<String, List<DriveObject>> grouped = new LinkedHashMap<>();
        long bytes = 0L;
        int count = 0;
        for (DriveObject object : safe) {
            if (object == null) { continue; }
            grouped.computeIfAbsent(object.getVirtualPath(), ignored -> new ArrayList<>()).add(object);
            bytes += object.getSize();
            count++;
        }
        return new DriveIndex(grouped, bytes, count);
    }

    public long getTotalBytes() { return totalBytes; }
    public int getFileCount() { return fileCount; }

    public List<DriveObject> getObjects(String directory) {
        String normalized = VirtualPath.normalize(directory);
        List<DriveObject> values = byDirectory.get(normalized);
        return values == null ? Collections.emptyList() : values;
    }

    public long getDirectoryBytes(String directory) {
        long bytes = 0L;
        for (DriveObject object : getObjects(directory)) {
            bytes += object.getSize();
        }
        return bytes;
    }

    public Map<String, List<DriveObject>> getDirectories() { return byDirectory; }
}
