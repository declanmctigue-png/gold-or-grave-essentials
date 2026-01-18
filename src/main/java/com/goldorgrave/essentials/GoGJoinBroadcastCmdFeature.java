// File: com/goldorgrave/essentials/GoGJoinBroadcastCmdFeature.java
package com.goldorgrave.essentials;

import com.goldorgrave.essentials.storage.Stores;
import com.goldorgrave.essentials.util.MessageUtil;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GoGJoinBroadcastCmdFeature {

    private static final String KEY = "joinbroadcast.template";

    private final Stores stores;

    public GoGJoinBroadcastCmdFeature(Stores stores) {
        this.stores = stores;
    }

    public CompletableFuture<Void> handleJoinBroadcast(CommandContext ctx, String[] args) {
        if (args.length < 2) {
            usage(ctx);
            return GoGInput.done();
        }

        if (GoGInput.eq(args[1], "set")) return handleSet(ctx, args);
        if (GoGInput.eq(args[1], "get")) return handleGet(ctx);
        if (GoGInput.eq(args[1], "clear")) return handleClear(ctx);

        usage(ctx);
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleSet(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.joinbroadcast.set")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.joinbroadcast.set");
            return GoGInput.done();
        }

        if (args.length < 3) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog joinbroadcast set <message...>");
            GoGInput.send(ctx, "&7[GoG] Tokens: &f%player% %user% %uuid%");
            GoGInput.send(ctx, "&7[GoG] Multiline: &fuse || or |n|");
            return GoGInput.done();
        }

        String template = GoGInput.joinFrom(args, 2);

        // Normalize multiline tokens into the one your broadcaster supports
        template = template.replace("|n|", "||");

        // This must persist inside Stores
        stores.setJoinBroadcastMessage(template);

        GoGInput.send(ctx, "&a[GoG] Set join broadcast template.");
        GoGInput.send(ctx, "&6[GoG] Template:");
        ctx.sender().sendMessage(MessageUtil.legacy(template));

        return GoGInput.done();
    }

    private CompletableFuture<Void> handleGet(CommandContext ctx) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.joinbroadcast.get")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.joinbroadcast.get");
            return GoGInput.done();
        }

        String template = stores.getJoinBroadcastMessage();
        GoGInput.send(ctx, "&6[GoG] Join broadcast template:");
        if (template == null || template.isBlank()) {
            GoGInput.send(ctx, "&7(none)");
        } else {
            ctx.sender().sendMessage(MessageUtil.legacy(template));
            GoGInput.send(ctx, "&7[GoG] Tokens resolve for the joining player at join time.");
        }
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleClear(CommandContext ctx) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.joinbroadcast.clear")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.joinbroadcast.clear");
            return GoGInput.done();
        }

        stores.setJoinBroadcastMessage("");
        GoGInput.send(ctx, "&a[GoG] Cleared join broadcast template.");
        return GoGInput.done();
    }

    private void usage(CommandContext ctx) {
        GoGInput.send(ctx, "&c[GoG] Usage: /gog joinbroadcast <set|get|clear> ...");
    }
}
