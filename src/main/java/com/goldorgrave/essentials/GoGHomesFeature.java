// File: com/goldorgrave/essentials/GoGHomesFeature.java
package com.goldorgrave.essentials;

import com.goldorgrave.essentials.perms.model.HomeLocation;
import com.goldorgrave.essentials.storage.Stores;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GoGHomesFeature {

    private final Stores stores;

    public GoGHomesFeature(Stores stores) {
        this.stores = stores;
    }

    public CompletableFuture<Void> handleHomesList(CommandContext ctx) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.homes")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.homes");
            return GoGInput.done();
        }

        Set<String> homes = stores.listHomes(uuid);
        if (homes.isEmpty()) {
            GoGInput.send(ctx, "&e[GoG] You have no homes set");
            return GoGInput.done();
        }

        GoGInput.send(ctx, "&6[GoG] Homes: &f" + String.join(", ", homes));
        return GoGInput.done();
    }

    public CompletableFuture<Void> handleSetHome(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.sethome")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.sethome");
            return GoGInput.done();
        }

        if (args.length < 2) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog sethome <name>");
            return GoGInput.done();
        }

        String name = args[1].toLowerCase(Locale.ROOT);

        return GoGTeleport.captureHomeLocationFromEntityStore(ctx).thenCompose(loc -> {
            if (loc == null) {
                GoGInput.send(ctx, "&c[GoG] Could not get your position.");
                GoGInput.send(ctx, "&7[GoG] If you are in limbo between worlds, try again after loading in.");
                return GoGInput.done();
            }
            stores.setHome(uuid, name, loc);
            GoGInput.send(ctx, "&a[GoG] Set home &f" + name);
            return GoGInput.done();
        });
    }

    public CompletableFuture<Void> handleHome(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.home")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.home");
            return GoGInput.done();
        }

        if (args.length < 2) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog home <name>");
            return GoGInput.done();
        }

        String name = args[1].toLowerCase(Locale.ROOT);
        HomeLocation loc = stores.getHome(uuid, name);
        if (loc == null) {
            GoGInput.send(ctx, "&c[GoG] Home not found: &f" + name);
            return GoGInput.done();
        }

        return GoGTeleport.teleportToHomeViaTeleportComponent(ctx, loc).thenCompose(ok -> {
            if (!ok) GoGInput.send(ctx, "&c[GoG] Teleport failed. Check console logs.");
            else GoGInput.send(ctx, "&a[GoG] Teleported to home &f" + name);
            return GoGInput.done();
        });
    }

    public CompletableFuture<Void> handleDelHome(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.delhome")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.delhome");
            return GoGInput.done();
        }

        if (args.length < 2) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog delhome <name>");
            return GoGInput.done();
        }

        String name = args[1].toLowerCase(Locale.ROOT);
        boolean ok = stores.delHome(uuid, name);
        if (!ok) GoGInput.send(ctx, "&c[GoG] Home not found: &f" + name);
        else GoGInput.send(ctx, "&a[GoG] Deleted home &f" + name);
        return GoGInput.done();
    }
}
