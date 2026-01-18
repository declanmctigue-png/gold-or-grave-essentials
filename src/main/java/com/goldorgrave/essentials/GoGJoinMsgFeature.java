package com.goldorgrave.essentials;

import com.goldorgrave.essentials.storage.Stores;
import com.goldorgrave.essentials.util.MessageUtil;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GoGJoinMsgFeature {

    private final Stores stores;

    public GoGJoinMsgFeature(Stores stores) {
        this.stores = stores;
    }

    // ----------------------------
    // Commands: /gog joinmsg ...
    // ----------------------------

    public CompletableFuture<Void> handleJoinMsg(CommandContext ctx, String[] args) {
        if (args.length < 2) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog joinmsg <set|get|clear> ...");
            return GoGInput.done();
        }
        if (GoGInput.eq(args[1], "set")) return handleJoinMsgSet(ctx, args);
        if (GoGInput.eq(args[1], "get")) return handleJoinMsgGet(ctx);
        if (GoGInput.eq(args[1], "clear")) return handleJoinMsgClear(ctx);
        GoGInput.send(ctx, "&c[GoG] Usage: /gog joinmsg <set|get|clear> ...");
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleJoinMsgSet(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.joinmsg.set")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.joinmsg.set");
            return GoGInput.done();
        }

        if (args.length < 3) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog joinmsg set <message...>");
            GoGInput.send(ctx, "&7[GoG] Tokens: &f%player% %uuid%");
            return GoGInput.done();
        }

        String msg = GoGInput.joinFrom(args, 2);
        stores.setJoinMessage(msg);

        GoGInput.send(ctx, "&a[GoG] Set join message to:");
        ctx.sender().sendMessage(MessageUtil.legacy(msg));
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleJoinMsgGet(CommandContext ctx) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.joinmsg.get")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.joinmsg.get");
            return GoGInput.done();
        }

        String msg = stores.getJoinMessage();
        GoGInput.send(ctx, "&6[GoG] Join message:");
        if (msg == null || msg.isBlank()) GoGInput.send(ctx, "&7(none)");
        else ctx.sender().sendMessage(MessageUtil.legacy(msg));
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleJoinMsgClear(CommandContext ctx) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.joinmsg.clear")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.joinmsg.clear");
            return GoGInput.done();
        }

        stores.setJoinMessage(null);
        GoGInput.send(ctx, "&a[GoG] Cleared join message.");
        return GoGInput.done();
    }

    // ----------------------------
    // Join hook: call this from PlayerConnectEvent
    // ----------------------------

    public void onPlayerJoin(Object playerRefOrPlayer) {
        if (playerRefOrPlayer == null) return;

        String template = stores.getJoinMessage();
        if (template == null || template.isBlank()) return;

        UUID uuid = tryGetUuid(playerRefOrPlayer);
        String username = tryGetUsername(playerRefOrPlayer);

        // If your connect event passes PlayerRef, uuid/username should be available.
        // Fall back to safe defaults.
        if (username == null || username.isBlank()) username = "player";

        String rendered = template
                .replace("%player%", username)
                .replace("%uuid%", uuid == null ? "" : uuid.toString());

        // Send ONLY to the joining player (original intention per your message).
        // Works if PlayerRef has sendMessage, or if the passed object is the player entity.
        if (!trySendMessage(playerRefOrPlayer, rendered)) {
            // If we can’t message them directly, you can optionally broadcast instead.
            // For now just log so you can see whether the build supports direct send.
            System.out.println("[GoG] Could not send join message to player object: " + playerRefOrPlayer.getClass().getName());
        }
    }

    private static boolean trySendMessage(Object target, String msg) {
        try {
            // Common patterns: sendMessage(Message) or sendMessage(String)
            Object r = GoGReflection.invoke(target, "sendMessage", MessageUtil.legacy(msg));
            if (r != null || hasMethod(target, "sendMessage", 1)) return true;
        } catch (Throwable ignored) {}

        try {
            Object r = GoGReflection.invoke(target, "sendMessage", msg);
            if (r != null || hasMethod(target, "sendMessage", 1)) return true;
        } catch (Throwable ignored) {}

        return false;
    }

    private static boolean hasMethod(Object obj, String name, int params) {
        try {
            for (var m : obj.getClass().getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == params) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static UUID tryGetUuid(Object o) {
        try {
            Object v = GoGReflection.invoke(o, "getUuid");
            return (v instanceof UUID u) ? u : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String tryGetUsername(Object o) {
        try {
            Object v = GoGReflection.invoke(o, "getUsername");
            return (v instanceof String s) ? s : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
