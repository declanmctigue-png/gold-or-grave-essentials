package com.goldorgrave.essentials;

import com.goldorgrave.essentials.storage.Stores;
import com.goldorgrave.essentials.util.MessageUtil;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class GoGJoinMsgListener {

    private final Stores stores;

    public GoGJoinMsgListener(Stores stores) {
        this.stores = stores;
    }

    // Register: getEventRegistry().register(PlayerConnectEvent.class, joinMsgListener::onJoin);
    public void onJoin(Object event) {
        if (event == null) return;

        Object playerRef = invoke0(event, "getPlayerRef");
        if (playerRef == null) playerRef = invoke0(event, "playerRef");

        Object player = invoke0(event, "getPlayer");
        if (player == null) player = invoke0(event, "player");

        // Identity should come from playerRef when possible
        Object identity = (playerRef != null) ? playerRef : player;

        // Sending can use either, prefer player first
        Object sendTarget = (player != null) ? player : playerRef;

        if (identity == null || sendTarget == null) return;

        UUID uuid = tryGetUuid(identity);
        String username = tryGetUsername(identity);

        sendJoinMessageDelayed(sendTarget, uuid, username);

        System.out.println("[GoG] identity=" + identity.getClass().getName() + " sendTarget=" + sendTarget.getClass().getName());

    }

    private void sendJoinMessageDelayed(Object sendTarget, UUID uuid, String username) {
        String template = stores.getJoinMessage();
        if (template == null || template.isBlank()) return;

        String name = (username == null || username.isBlank()) ? "player" : username;

        String rendered = render(template, name, uuid);

        CompletableFuture.runAsync(
                () -> {
                    if (!trySendMessage(sendTarget, rendered)) {
                        System.out.println("[GoG] JoinMsg: sendMessage not supported on " + sendTarget.getClass().getName());
                    }
                },
                CompletableFuture.delayedExecutor(300, TimeUnit.MILLISECONDS)
        );
    }

    private static String render(String template, String username, UUID uuid) {
        String out = template;

        // multiline tokens
        out = out.replace("||", "\n");
        out = out.replace("|n|", "\n");

        // variables, case insensitive
        out = replaceIgnoreCase(out, "%player%", username);
        out = replaceIgnoreCase(out, "%user%", username);
        out = replaceIgnoreCase(out, "%uuid%", uuid == null ? "" : uuid.toString());

        return out;
    }

    private static String replaceIgnoreCase(String input, String needle, String replacement) {
        if (input == null || needle == null || needle.isEmpty()) return input;
        if (replacement == null) replacement = "";

        String lower = input.toLowerCase(Locale.ROOT);
        String n = needle.toLowerCase(Locale.ROOT);

        StringBuilder sb = new StringBuilder(input.length());
        int i = 0;
        while (true) {
            int idx = lower.indexOf(n, i);
            if (idx < 0) {
                sb.append(input, i, input.length());
                break;
            }
            sb.append(input, i, idx);
            sb.append(replacement);
            i = idx + needle.length();
        }
        return sb.toString();
    }

    private static boolean trySendMessage(Object target, String msg) {
        if (target == null) return false;

        try {
            Method m = target.getClass().getMethod("sendMessage", com.hypixel.hytale.server.core.Message.class);
            m.setAccessible(true);
            m.invoke(target, MessageUtil.legacy(msg));
            return true;
        } catch (Throwable ignored) {}

        try {
            Method m = target.getClass().getMethod("sendMessage", String.class);
            m.setAccessible(true);
            m.invoke(target, msg);
            return true;
        } catch (Throwable ignored) {}

        return false;
    }

    private static UUID tryGetUuid(Object o) {
        Object v = invoke0(o, "getUuid");
        if (v instanceof UUID u) return u;

        // some builds use getId
        v = invoke0(o, "getId");
        return (v instanceof UUID u2) ? u2 : null;
    }

    private static String tryGetUsername(Object o) {
        Object v = invoke0(o, "getUsername");
        if (v instanceof String s) return s;

        // some builds use getName
        v = invoke0(o, "getName");
        if (v instanceof String s2) return s2;

        // some builds use name()
        v = invoke0(o, "name");
        return (v instanceof String s3) ? s3 : null;
    }

    private static Object invoke0(Object obj, String method) {
        if (obj == null) return null;
        try {
            Method m = obj.getClass().getMethod(method);
            m.setAccessible(true);
            return m.invoke(obj);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
