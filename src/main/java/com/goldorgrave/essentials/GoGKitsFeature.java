// File: com/goldorgrave/essentials/GoGKitsFeature.java
package com.goldorgrave.essentials;

import com.goldorgrave.essentials.perms.model.Kit;
import com.goldorgrave.essentials.perms.model.KitItem;
import com.goldorgrave.essentials.storage.Stores;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GoGKitsFeature {

    private final Stores stores;

    public GoGKitsFeature(Stores stores) {
        this.stores = stores;
    }

    public CompletableFuture<Void> handleKitsList(CommandContext ctx) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.kits.list")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.kits.list");
            return GoGInput.done();
        }

        Set<String> kits = stores.listKits();
        if (kits.isEmpty()) {
            GoGInput.send(ctx, "&e[GoG] No kits exist");
            return GoGInput.done();
        }

        GoGInput.send(ctx, "&6[GoG] Kits: &f" + String.join(", ", kits));
        GoGInput.send(ctx, "&7[GoG] Use: &f/gog kit <name>");
        return GoGInput.done();
    }

    public CompletableFuture<Void> handleKit(CommandContext ctx, String[] args) {
        if (args.length < 2) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog kit <name>");
            GoGInput.send(ctx, "&c[GoG] Admin: /gog kit create|delete|give ...");
            return GoGInput.done();
        }

        if (GoGInput.eq(args[1], "create")) return handleKitCreate(ctx, args);
        if (GoGInput.eq(args[1], "delete")) return handleKitDelete(ctx, args);
        if (GoGInput.eq(args[1], "give")) return handleKitGive(ctx, args);

        return handleKitUse(ctx, args[1]);
    }

    private CompletableFuture<Void> handleKitCreate(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.kit.create")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.kit.create");
            return GoGInput.done();
        }

        if (args.length < 3) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog kit create <name> [cooldownSeconds] [permissionNode]");
            return GoGInput.done();
        }

        String kitName = args[2];
        long cooldownSeconds = 0;
        if (args.length >= 4) cooldownSeconds = GoGInput.parseLong(args[3], 0);

        String permissionNode = "gog.kit." + kitName.toLowerCase(Locale.ROOT);
        if (args.length >= 5) permissionNode = args[4];

        List<KitItem> items = GoGKitsUtil.snapshotInventoryAsKitItems(ctx.sender());
        if (items == null || items.isEmpty()) {
            GoGInput.send(ctx, "&c[GoG] Could not read inventory or inventory empty");
            return GoGInput.done();
        }

        Kit kit = new Kit(kitName, permissionNode, cooldownSeconds, items);
        stores.upsertKit(kit);

        GoGInput.send(ctx, "&a[GoG] Saved kit &f" + kitName + " &awith &f" + items.size() + " &aitems");
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleKitDelete(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.kit.delete")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.kit.delete");
            return GoGInput.done();
        }

        if (args.length < 3) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog kit delete <name>");
            return GoGInput.done();
        }

        boolean ok = stores.deleteKit(args[2]);
        if (!ok) GoGInput.send(ctx, "&c[GoG] Kit not found: &f" + args[2]);
        else GoGInput.send(ctx, "&a[GoG] Deleted kit &f" + args[2]);
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleKitGive(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.kit.give")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.kit.give");
            return GoGInput.done();
        }

        if (args.length < 3) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog kit give <kit> [playerName]");
            return GoGInput.done();
        }

        Kit kit = stores.getKit(args[2]);
        if (kit == null) {
            GoGInput.send(ctx, "&c[GoG] Kit not found: &f" + args[2]);
            return GoGInput.done();
        }

        Object target = ctx.sender();
        if (args.length >= 4) {
            Object found = GoGPlayers.findPlayerEntityByName(ctx, args[3]);
            if (found == null) {
                GoGInput.send(ctx, "&c[GoG] Player not found: &f" + args[3]);
                return GoGInput.done();
            }
            target = found;
        }

        boolean ok = GoGKitsUtil.giveKitToPlayer(target, kit);
        if (!ok) {
            GoGInput.send(ctx, "&c[GoG] Failed to give kit");
            return GoGInput.done();
        }

        GoGInput.send(ctx, "&a[GoG] Gave kit &f" + kit.name());
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleKitUse(CommandContext ctx, String kitName) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        Kit kit = stores.getKit(kitName);
        if (kit == null) {
            GoGInput.send(ctx, "&c[GoG] Kit not found: &f" + kitName);
            return GoGInput.done();
        }

        String perm = kit.permission();
        if (perm != null && !perm.isBlank() && !GoGPermissions.has(ctx, stores, uuid, perm)) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f" + perm);
            return GoGInput.done();
        }

        long now = System.currentTimeMillis();
        long nextOk = stores.nextKitAllowed(uuid, kit.name());
        if (nextOk > now) {
            long seconds = Math.max(1, (nextOk - now) / 1000);
            GoGInput.send(ctx, "&e[GoG] You can use that kit again in &f" + seconds + " &eseconds");
            return GoGInput.done();
        }

        boolean ok = GoGKitsUtil.giveKitToPlayer(ctx.sender(), kit);
        if (!ok) {
            GoGInput.send(ctx, "&c[GoG] Failed to give kit");
            return GoGInput.done();
        }

        long cd = kit.cooldownSeconds();
        if (cd > 0) stores.setKitCooldown(uuid, kit.name(), now + (cd * 1000L));

        GoGInput.send(ctx, "&a[GoG] Kit received: &f" + kit.name());
        return GoGInput.done();
    }
}
