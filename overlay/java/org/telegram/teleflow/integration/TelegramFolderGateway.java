/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.support.LongSparseIntArray;
import org.telegram.teleflow.folders.FolderServerSnapshot;
import org.telegram.teleflow.folders.FolderUpdatePlan;
import org.telegram.teleflow.folders.ManagedFolderSpec;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;

public final class TelegramFolderGateway {
    public interface ResultCallback {
        void onResult(boolean success, String error);
    }

    public interface CreateCallback {
        void onResult(boolean success, MessagesController.DialogFilter filter, String error);
    }

    private TelegramFolderGateway() {}

    public static FolderServerSnapshot snapshot(MessagesController.DialogFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("filter is required");
        }
        return new FolderServerSnapshot(
            filter.id,
            filter.name,
            new ArrayList<>(filter.alwaysShow),
            orderedPinned(filter.pinnedDialogs),
            new ArrayList<>(filter.neverShow)
        );
    }

    public static void applyExisting(BaseFragment fragment, MessagesController.DialogFilter filter, FolderUpdatePlan plan, ResultCallback callback) {
        if (fragment == null || filter == null || plan == null || callback == null) {
            throw new IllegalArgumentException("fragment, filter, plan and callback are required");
        }
        if (filter.id != plan.getFolderId()) {
            throw new IllegalArgumentException("filter id does not match update plan");
        }
        send(fragment, filter, plan.getIncludeDialogIds(), plan.getPinnedDialogIds(), plan.getExcludeDialogIds(), false, callback);
    }

    public static void restoreSnapshot(BaseFragment fragment, MessagesController.DialogFilter filter, FolderServerSnapshot snapshot, ResultCallback callback) {
        if (fragment == null || filter == null || snapshot == null || callback == null) {
            throw new IllegalArgumentException("fragment, filter, snapshot and callback are required");
        }
        if (filter.id != snapshot.getFolderId()) {
            throw new IllegalArgumentException("filter id does not match backup snapshot");
        }
        send(fragment, filter, snapshot.getIncludeDialogIds(), snapshot.getPinnedDialogIds(), snapshot.getExcludeDialogIds(), false, callback);
    }

    public static void createNew(BaseFragment fragment, ManagedFolderSpec spec, List<Long> includeIds, CreateCallback callback) {
        if (fragment == null || spec == null || callback == null) {
            throw new IllegalArgumentException("fragment, spec and callback are required");
        }
        MessagesController controller = fragment.getMessagesController();
        MessagesController.DialogFilter filter = new MessagesController.DialogFilter();
        filter.id = allocateFilterId(controller);
        filter.name = spec.getServerName();
        filter.color = 0;
        filter.flags = 0;
        filter.entities = new ArrayList<>();
        filter.title_noanimate = false;
        send(
            fragment,
            filter,
            includeIds == null ? Collections.emptyList() : includeIds,
            Collections.emptyList(),
            Collections.emptyList(),
            true,
            (success, error) -> callback.onResult(success, success ? filter : null, error)
        );
    }

    public static void deleteFolder(BaseFragment fragment, MessagesController.DialogFilter filter, ResultCallback callback) {
        if (fragment == null || filter == null || callback == null) {
            throw new IllegalArgumentException("fragment, filter and callback are required");
        }
        TLRPC.TL_messages_updateDialogFilter req = new TLRPC.TL_messages_updateDialogFilter();
        req.id = filter.id;
        fragment.getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error != null) {
                callback.onResult(false, error.text == null ? "telegram-rpc-error" : error.text);
                return;
            }
            if (response == null) {
                callback.onResult(false, "telegram-empty-response");
                return;
            }
            fragment.getMessagesController().removeFilter(filter);
            fragment.getMessagesStorage().deleteDialogFilter(filter);
            fragment.getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            callback.onResult(true, null);
        }));
    }

    static int allocateFilterId(MessagesController controller) {
        int id = 2;
        while (controller.dialogFiltersById.get(id) != null) {
            id++;
        }
        return id;
    }

    private static void send(
        BaseFragment fragment,
        MessagesController.DialogFilter filter,
        List<Long> includeIds,
        List<Long> pinnedIds,
        List<Long> excludeIds,
        boolean creatingNew,
        ResultCallback callback
    ) {
        TLRPC.TL_messages_updateDialogFilter req = new TLRPC.TL_messages_updateDialogFilter();
        req.id = filter.id;
        req.flags |= 1;
        req.filter = new TLRPC.TL_dialogFilter();
        req.filter.contacts = (filter.flags & MessagesController.DIALOG_FILTER_FLAG_CONTACTS) != 0;
        req.filter.non_contacts = (filter.flags & MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS) != 0;
        req.filter.groups = (filter.flags & MessagesController.DIALOG_FILTER_FLAG_GROUPS) != 0;
        req.filter.broadcasts = (filter.flags & MessagesController.DIALOG_FILTER_FLAG_CHANNELS) != 0;
        req.filter.bots = (filter.flags & MessagesController.DIALOG_FILTER_FLAG_BOTS) != 0;
        req.filter.exclude_muted = (filter.flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) != 0;
        req.filter.exclude_read = (filter.flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ) != 0;
        req.filter.exclude_archived = (filter.flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) != 0;
        req.filter.id = filter.id;
        req.filter.title = new TLRPC.TL_textWithEntities();
        req.filter.title.text = filter.name;
        req.filter.title.entities = filter.entities == null ? new ArrayList<>() : filter.entities;
        req.filter.title_noanimate = filter.title_noanimate;
        if (filter.color < 0) {
            req.filter.flags &= ~134217728;
            req.filter.color = 0;
        } else {
            req.filter.flags |= 134217728;
            req.filter.color = filter.color;
        }

        MessagesController controller = fragment.getMessagesController();
        LongSparseIntArray pinnedMap = toPinnedMap(pinnedIds);
        Long unresolved = appendPeers(controller, includeIds, req.filter.include_peers, pinnedMap, true);
        if (unresolved == null) {
            unresolved = appendPeers(controller, excludeIds, req.filter.exclude_peers, pinnedMap, false);
        }
        if (unresolved == null) {
            unresolved = appendPeers(controller, pinnedIds, req.filter.pinned_peers, pinnedMap, false);
        }
        if (unresolved != null) {
            callback.onResult(false, "unresolved-dialog:" + unresolved);
            return;
        }

        fragment.getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error != null) {
                callback.onResult(false, error.text == null ? "telegram-rpc-error" : error.text);
                return;
            }
            if (response == null) {
                callback.onResult(false, "telegram-empty-response");
                return;
            }

            filter.pendingUnreadCount = -1;
            filter.unreadCount = -1;
            filter.alwaysShow = new ArrayList<>(includeIds);
            filter.neverShow = new ArrayList<>(excludeIds);
            filter.pinnedDialogs.clear();
            for (int i = 0; i < pinnedIds.size(); i++) {
                Long id = pinnedIds.get(i);
                if (id != null) {
                    filter.pinnedDialogs.put(id, i);
                }
            }
            if (creatingNew) {
                controller.addFilter(filter, false);
            } else {
                controller.onFilterUpdate(filter);
            }
            fragment.getMessagesStorage().saveDialogFilter(filter, false, true);
            fragment.getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            callback.onResult(true, null);
        }));
    }

    private static Long appendPeers(
        MessagesController controller,
        List<Long> ids,
        ArrayList<TLRPC.InputPeer> destination,
        LongSparseIntArray pinned,
        boolean skipPinned
    ) {
        for (Long value : ids) {
            if (value == null) {
                continue;
            }
            long did = value;
            if (skipPinned && pinned.indexOfKey(did) >= 0) {
                continue;
            }
            if (DialogObject.isEncryptedDialog(did)) {
                return did;
            }
            if (did > 0) {
                TLRPC.User user = controller.getUser(did);
                if (user == null) {
                    return did;
                }
                TLRPC.TL_inputPeerUser inputPeer = new TLRPC.TL_inputPeerUser();
                inputPeer.user_id = did;
                inputPeer.access_hash = user.access_hash;
                destination.add(inputPeer);
            } else {
                TLRPC.Chat chat = controller.getChat(-did);
                if (chat == null) {
                    return did;
                }
                if (ChatObject.isChannel(chat)) {
                    TLRPC.TL_inputPeerChannel inputPeer = new TLRPC.TL_inputPeerChannel();
                    inputPeer.channel_id = -did;
                    inputPeer.access_hash = chat.access_hash;
                    destination.add(inputPeer);
                } else {
                    TLRPC.TL_inputPeerChat inputPeer = new TLRPC.TL_inputPeerChat();
                    inputPeer.chat_id = -did;
                    destination.add(inputPeer);
                }
            }
        }
        return null;
    }

    private static LongSparseIntArray toPinnedMap(List<Long> pinnedIds) {
        LongSparseIntArray pinned = new LongSparseIntArray(pinnedIds == null ? 0 : pinnedIds.size());
        if (pinnedIds != null) {
            for (int i = 0; i < pinnedIds.size(); i++) {
                Long id = pinnedIds.get(i);
                if (id != null) {
                    pinned.put(id, i);
                }
            }
        }
        return pinned;
    }

    private static ArrayList<Long> orderedPinned(LongSparseIntArray pinned) {
        ArrayList<long[]> pairs = new ArrayList<>();
        if (pinned != null) {
            for (int i = 0; i < pinned.size(); i++) {
                pairs.add(new long[] { pinned.keyAt(i), pinned.valueAt(i) });
            }
        }
        pairs.sort(Comparator.comparingLong(value -> value[1]));
        ArrayList<Long> result = new ArrayList<>(pairs.size());
        for (long[] pair : pairs) {
            result.add(pair[0]);
        }
        return result;
    }
}
