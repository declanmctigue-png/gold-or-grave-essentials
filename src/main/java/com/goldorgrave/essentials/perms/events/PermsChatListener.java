// File: com/goldorgrave/essentials/perms/events/PermsChatListener.java
package com.goldorgrave.essentials.perms.events;

import com.goldorgrave.essentials.perms.PermissionManager;
import com.goldorgrave.essentials.perms.models.ChatMeta;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import java.util.List;
import java.util.Locale;

public final class PermsChatListener {

    private final PermissionManager perms;

    public PermsChatListener(PermissionManager perms) {
        this.perms = perms;
    }

    public void onPlayerChat(PlayerChatEvent event) {
        if (event == null) return;

        PlayerRef sender = event.getSender();
        if (sender == null) return;
        if (sender.getUuid() == null) return;

        perms.ensureUserLoaded(sender.getUuid(), sender.getUsername());

        ChatMeta meta = perms.getChatMeta(sender.getUuid());

        event.setCancelled(true);

        Message out = Message.raw("");

        System.out.println("[GoG] ChatListener fired sender=" + event.getSender().getUsername());


        // Prefix
        String prefix = safe(meta == null ? null : meta.getPrefix());
        if (!prefix.isEmpty()) {
            Message pMsg = Message.raw(prefix + " ");
            String pColor = safe(meta == null ? null : meta.getPrefixColor());
            if (!pColor.isEmpty()) pMsg = pMsg.color(pColor);
            applyFormats(pMsg, safe(meta == null ? null : meta.getPrefixFormat()));
            out = out.insert(pMsg);
        }

        // Username
        Message uMsg = Message.raw(safe(sender.getUsername()));
        String uColor = safe(meta == null ? null : meta.getPlayerColor());
        if (!uColor.isEmpty()) uMsg = uMsg.color(uColor);
        applyFormats(uMsg, safe(meta == null ? null : meta.getPlayerFormat()));
        out = out.insert(uMsg);

        // Suffix
        String suffix = safe(meta == null ? null : meta.getSuffix());
        if (!suffix.isEmpty()) {
            Message sMsg = Message.raw(" " + suffix);
            String sColor = safe(meta == null ? null : meta.getSuffixColor());
            if (!sColor.isEmpty()) sMsg = sMsg.color(sColor);
            applyFormats(sMsg, safe(meta == null ? null : meta.getSuffixFormat()));
            out = out.insert(sMsg);
        }

        // Content
        Message cMsg = Message.raw(": " + safe(event.getContent()));
        String cColor = safe(meta == null ? null : meta.getChatColor());
        if (!cColor.isEmpty()) cMsg = cMsg.color(cColor);
        applyFormats(cMsg, safe(meta == null ? null : meta.getChatFormat()));
        out = out.insert(cMsg);

        // Broadcast
        List<PlayerRef> players = Universe.get().getPlayers();
        if (players == null) return;
        for (PlayerRef pr : players) {
            if (pr == null) continue;
            pr.sendMessage(out);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static void applyFormats(Message msg, String formatString) {
        if (msg == null) return;
        if (formatString == null || formatString.isBlank()) return;

        String[] parts = formatString.split(",");
        for (String p : parts) {
            String f = p == null ? "" : p.trim().toLowerCase(Locale.ROOT);
            if (f.equals("bold")) msg.bold(true);
            if (f.equals("italic")) msg.italic(true);
        }
    }
}
