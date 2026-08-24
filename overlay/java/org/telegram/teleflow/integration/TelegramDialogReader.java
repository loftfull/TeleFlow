/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.integration;

import java.util.ArrayList;
import java.util.List;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserObject;
import org.telegram.teleflow.folders.ChatDescriptor;
import org.telegram.tgnet.TLRPC;

public final class TelegramDialogReader {
    private TelegramDialogReader() {}

    public static List<ChatDescriptor> readLoadedDialogs(MessagesController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("controller is required");
        }
        ArrayList<ChatDescriptor> result = new ArrayList<>();
        for (int i = 0; i < controller.dialogs_dict.size(); i++) {
            TLRPC.Dialog dialog = controller.dialogs_dict.valueAt(i);
            if (dialog == null || dialog.isFolder || DialogObject.isEncryptedDialog(dialog.id)) {
                continue;
            }
            ChatDescriptor descriptor = descriptorFor(controller, dialog.id);
            if (descriptor != null) {
                result.add(descriptor);
            }
        }
        return result;
    }

    private static ChatDescriptor descriptorFor(MessagesController controller, long dialogId) {
        if (dialogId > 0) {
            TLRPC.User user = controller.getUser(dialogId);
            if (user == null) { return null; }
            return new ChatDescriptor(
                dialogId,
                UserObject.getUserName(user),
                user.username,
                user.bot ? ChatDescriptor.PeerType.BOT : ChatDescriptor.PeerType.USER
            );
        }

        TLRPC.Chat chat = controller.getChat(-dialogId);
        if (chat == null) { return null; }
        boolean broadcastChannel = ChatObject.isChannel(chat) && !chat.megagroup;
        return new ChatDescriptor(
            dialogId,
            chat.title,
            chat.username,
            broadcastChannel ? ChatDescriptor.PeerType.CHANNEL : ChatDescriptor.PeerType.GROUP
        );
    }
}
