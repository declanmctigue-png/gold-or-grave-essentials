package com.goldorgrave.essentials;

import com.goldorgrave.essentials.perms.models.ChatMeta;
import com.goldorgrave.essentials.storage.Stores;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import java.util.UUID;

public final class GoGChatListener {

    private final Stores stores;

    public GoGChatListener(Stores stores) {
        this.stores = stores;
    }

    public void onPlayerChat(PlayerChatEvent event) {
        if (event == null) return;

        PlayerRef sender = event.getSender();
        if (sender == null) return;

        // Cancel default chat
        event.setCancelled(true);

        UUID uuid = sender.getUuid();
        String username = sender.getUsername();
        if (uuid == null) return;
        if (username == null) username = "player";

        stores.permManager().ensureUserLoaded(uuid, username);

        String content = event.getContent();
        if (content == null) content = "";

        ChatMeta meta = stores.permManager().getChatMeta(uuid);

        com.hypixel.hytale.server.core.Message out =
                com.goldorgrave.essentials.util.MessageUtil.chatLine(meta, username, content);

        for (PlayerRef pr : Universe.get().getPlayers()) {
            pr.sendMessage(out);
        }
    }
}
