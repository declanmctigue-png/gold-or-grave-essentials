package com.goldorgrave.essentials.util;

import com.goldorgrave.essentials.perms.models.ChatMeta;
import com.hypixel.hytale.server.core.Message;

import java.awt.Color;

public final class MessageUtil {
    private MessageUtil() {}

    public static Message legacy(String input) {
        if (input == null) return Message.raw("");

        Message out = Message.empty();

        String s = input.replace("&&", "\u0000");
        Color current = null;

        StringBuilder buf = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);

            if (c == '&' && i + 1 < s.length()) {
                char code = Character.toLowerCase(s.charAt(i + 1));
                Color mapped = legacyColor(code);

                if (mapped != null || code == 'r') {
                    if (buf.length() > 0) {
                        Message part = Message.raw(buf.toString().replace("\u0000", "&"));
                        if (current != null) part.color(current);
                        out.insert(part);
                        buf.setLength(0);
                    }

                    current = (code == 'r') ? null : mapped;
                    i += 2;
                    continue;
                }
            }

            buf.append(c);
            i++;
        }

        if (buf.length() > 0) {
            Message part = Message.raw(buf.toString().replace("\u0000", "&"));
            if (current != null) part.color(current);
            out.insert(part);
        }

        return out;
    }

    public static Message chatLine(ChatMeta meta, String username, String content) {
        if (meta == null) meta = ChatMeta.empty();
        if (username == null) username = "player";
        if (content == null) content = "";

        Message out = Message.empty();

        String prefix = safe(meta.getPrefix());
        String prefixColor = safe(meta.getPrefixColor());

        if (!prefix.isBlank() || !prefixColor.isBlank()) {
            String prefixCombined = prefixColor + prefix + "&r";
            out.insert(legacy(prefixCombined));
            out.insert(Message.raw(" "));
        }

        String playerColor = safe(meta.getPlayerColor());
        if (!playerColor.isBlank()) out.insert(legacy(playerColor + username + "&r"));
        else out.insert(Message.raw(username));

        out.insert(Message.raw(": "));

        String chatColor = safe(meta.getChatColor());
        if (!chatColor.isBlank()) out.insert(legacy(chatColor + content + "&r"));
        else out.insert(Message.raw(content));

        // Prefix: if prefix contains & codes, render it as-is.
// Otherwise, if prefixColor exists, apply it to the whole prefix.D
        if (!prefix.isBlank()) {
            String pfxColor = safe(meta.getPrefixColor());

            String toRender;
            if (containsLegacyCode(prefix)) {
                toRender = prefix;
            } else if (!pfxColor.isBlank()) {
                toRender = pfxColor + prefix;
            } else {
                toRender = prefix;
            }

        }


        return out;
    }
    private static boolean containsLegacyCode(String s) {
        if (s == null) return false;
        for (int i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == '&') {
                char c = Character.toLowerCase(s.charAt(i + 1));
                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || c == 'r') return true;
            }
        }
        return false;
    }


    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static Color legacyColor(char code) {
        return switch (code) {
            case '0' -> new Color(0x000000);
            case '1' -> new Color(0x0000AA);
            case '2' -> new Color(0x00AA00);
            case '3' -> new Color(0x00AAAA);
            case '4' -> new Color(0xAA0000);
            case '5' -> new Color(0xAA00AA);
            case '6' -> new Color(0xFFAA00);
            case '7' -> new Color(0xAAAAAA);
            case '8' -> new Color(0x555555);
            case '9' -> new Color(0x5555FF);
            case 'a' -> new Color(0x55FF55);
            case 'b' -> new Color(0x55FFFF);
            case 'c' -> new Color(0xFF5555);
            case 'd' -> new Color(0xFF55FF);
            case 'e' -> new Color(0xFFFF55);
            case 'f' -> new Color(0xFFFFFF);
            default -> null;
        };
    }
}
