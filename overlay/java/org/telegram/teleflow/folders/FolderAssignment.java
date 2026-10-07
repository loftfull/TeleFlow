/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

public final class FolderAssignment {
    private final ChatDescriptor chat;
    private final ClassificationDecision decision;

    public FolderAssignment(ChatDescriptor chat, ClassificationDecision decision) {
        if (chat == null || decision == null) {
            throw new IllegalArgumentException("chat and decision are required");
        }
        this.chat = chat;
        this.decision = decision;
    }

    public ChatDescriptor getChat() { return chat; }
    public ClassificationDecision getDecision() { return decision; }
}
