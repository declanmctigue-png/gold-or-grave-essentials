package com.goldorgrave.essentials.perms.utils;

import com.goldorgrave.essentials.perms.models.ChatMeta;
import com.hypixel.hytale.server.core.Message;

import java.awt.Color;

public final class MessageFactory {
    private MessageFactory() {}

    public static Message buildChatMessage(ChatMeta meta, String username, String content) {
        if (meta == null) meta = ChatMeta.empty();
        if (username == null || username.isBlank()) username = "player";
        if (content == null) content = "";

        Message out = Message.empty();

        // Prefix
        if (!meta.getPrefix().isBlank()) {
            Message p = Message.raw(meta.getPrefix());
            applyColorIfPresent(p, meta.getPrefixColor());
            out.insert(p);
            out.insert(Message.raw(" "));
        }

        // Player name
        Message name = Message.raw(username);
        applyColorIfPresent(name, meta.getPlayerColor());
        out.insert(name);

        // Suffix (optional)
        if (!meta.getSuffix().isBlank()) {
            out.insert(Message.raw(" "));
            Message s = Message.raw(meta.getSuffix());
            applyColorIfPresent(s, meta.getSuffixColor());
            out.insert(s);
        }

        // Separator
        out.insert(Message.raw(": "));

        // Chat content
        Message msg = Message.raw(content);
        applyColorIfPresent(msg, meta.getChatColor());
        out.insert(msg);

        return out;
    }

    private static void applyColorIfPresent(Message m, String colorStr) {
        Color c = parseColor(colorStr);
        if (c != null) {
            m.color(c);
        }
    }

    // Accepts "#RRGGBB" or "RRGGBB". Returns null if empty or invalid.
    private static Color parseColor(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() != 6) return null;

        try {
            int rgb = Integer.parseInt(s, 16);
            return new Color(rgb);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
