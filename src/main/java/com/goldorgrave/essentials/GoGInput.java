// File: com/goldorgrave/essentials/GoGInput.java
package com.goldorgrave.essentials;

import com.goldorgrave.essentials.util.MessageUtil;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class GoGInput {
    private GoGInput() {}

    public static String[] getArgs(CommandContext ctx) {
        String input = getInputString(ctx);
        if (input == null) return new String[0];

        String s = input.trim();
        if (s.startsWith("/")) s = s.substring(1);

        String[] tokens = s.split("\\s+");
        if (tokens.length <= 1) return new String[0];

        String[] out = new String[tokens.length - 1];
        System.arraycopy(tokens, 1, out, 0, out.length);
        return out;
    }

    public static String getInputString(CommandContext ctx) {
        String input = tryStringMethod(ctx, "getInputString");
        if (input == null) input = tryStringMethod(ctx, "inputString");
        if (input == null) input = tryStringMethod(ctx, "getInput");
        if (input == null) input = tryStringMethod(ctx, "input");
        return input;
    }

    private static String tryStringMethod(Object obj, String name) {
        try {
            Method m = obj.getClass().getMethod(name);
            Object v = m.invoke(obj);
            return v instanceof String ? (String) v : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean eq(String a, String b) {
        return a != null && a.equalsIgnoreCase(b);
    }

    public static long parseLong(String s, long def) {
        try { return Long.parseLong(s); } catch (Throwable e) { return def; }
    }

    public static void send(CommandContext ctx, String text) {
        ctx.sender().sendMessage(MessageUtil.legacy(text));
    }

    public static String joinFrom(String[] args, int start) {
        if (args == null || start < 0 || start >= args.length) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    public static CompletableFuture<Void> done() {
        return CompletableFuture.completedFuture(null);
    }

    public static String safe(String s) {
        return s == null ? "" : s;
    }

    public static boolean isLegacyColorCode(String s) {
        if (s == null) return false;
        s = s.trim();
        if (s.length() != 2) return false;
        if (s.charAt(0) != '&') return false;
        char c = Character.toLowerCase(s.charAt(1));
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
    }

    // Extracts everything after "/gog rank prefix set <rank> "
    // Preserves raw characters, including & and &&.
    public static String extractPrefixFromRaw(String raw, String rankName) {
        if (raw == null) return null;

        String s = raw.trim();
        if (s.startsWith("/")) s = s.substring(1);

        String lower = s.toLowerCase(Locale.ROOT);
        String rn = rankName == null ? "" : rankName.toLowerCase(Locale.ROOT);

        String marker1 = ("gog rank prefix set " + rn + " ").toLowerCase(Locale.ROOT);
        int idx = lower.indexOf(marker1);
        if (idx >= 0) {
            int start = idx + marker1.length();
            if (start >= s.length()) return null;
            return s.substring(start);
        }

        String marker2 = ("rank prefix set " + rn + " ").toLowerCase(Locale.ROOT);
        idx = lower.indexOf(marker2);
        if (idx >= 0) {
            int start = idx + marker2.length();
            if (start >= s.length()) return null;
            return s.substring(start);
        }

        return null;
    }
}
