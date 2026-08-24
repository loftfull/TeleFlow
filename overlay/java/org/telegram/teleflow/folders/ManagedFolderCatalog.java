/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ManagedFolderCatalog {
    private ManagedFolderCatalog() {}

    public static List<ManagedFolderSpec> create() {
        ArrayList<ManagedFolderSpec> specs = new ArrayList<>();
        specs.add(new ManagedFolderSpec("ai-tech", "AI & Tech", "TF AI & Tech"));
        specs.add(new ManagedFolderSpec("finance", "Финансы", "TF Финансы"));
        specs.add(new ManagedFolderSpec("auto", "Авто", "TF Авто"));
        specs.add(new ManagedFolderSpec("news", "Новости", "TF Новости"));
        specs.add(new ManagedFolderSpec("shopping", "Покупки", "TF Покупки"));
        specs.add(new ManagedFolderSpec("learning", "Обучение", "TF Обучение"));
        specs.add(new ManagedFolderSpec("bots", "Боты", "TF Боты"));
        specs.add(new ManagedFolderSpec("review", "Разобрать", "TF Разобрать"));
        return Collections.unmodifiableList(specs);
    }

    public static ManagedFolderSpec byDisplayName(String displayName) {
        if (displayName == null) return null;
        for (ManagedFolderSpec spec : create()) {
            if (spec.getDisplayName().equals(displayName)) return spec;
        }
        return null;
    }

    public static ManagedFolderSpec byLogicalKey(String logicalKey) {
        if (logicalKey == null) return null;
        for (ManagedFolderSpec spec : create()) {
            if (spec.getLogicalKey().equals(logicalKey)) return spec;
        }
        return null;
    }
}
