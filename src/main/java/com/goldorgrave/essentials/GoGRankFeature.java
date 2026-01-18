// File: com/goldorgrave/essentials/GoGRankFeature.java
package com.goldorgrave.essentials;

import com.goldorgrave.essentials.perms.PermissionManager;
import com.goldorgrave.essentials.perms.models.Group;
import com.goldorgrave.essentials.perms.models.Meta;
import com.goldorgrave.essentials.storage.Stores;
import com.goldorgrave.essentials.util.MessageUtil;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GoGRankFeature {

    private final Stores stores;

    public GoGRankFeature(Stores stores) {
        this.stores = stores;
    }

    public CompletableFuture<Void> handleRanksList(CommandContext ctx) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.ranks")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.ranks");
            return GoGInput.done();
        }

        Set<String> groups = stores.permManager().listGroups();
        GoGInput.send(ctx, "&6[GoG] Ranks: &f" + String.join(", ", groups));
        return GoGInput.done();
    }

    public CompletableFuture<Void> handleRank(CommandContext ctx, String[] args) {
        if (args.length < 2) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog rank <create|delete|perm|set|get|prefix|prefixcolor> ...");
            return GoGInput.done();
        }

        if (GoGInput.eq(args[1], "create")) return handleRankCreate(ctx, args);
        if (GoGInput.eq(args[1], "delete")) return handleRankDelete(ctx, args);
        if (GoGInput.eq(args[1], "set")) return handleRankSet(ctx, args);
        if (GoGInput.eq(args[1], "get")) return handleRankGet(ctx, args);

        if (GoGInput.eq(args[1], "perm")) return handleRankPerm(ctx, args);
        if (GoGInput.eq(args[1], "prefix")) return handleRankPrefix(ctx, args);
        if (GoGInput.eq(args[1], "prefixcolor")) return handleRankPrefixColor(ctx, args);

        GoGInput.send(ctx, "&c[GoG] Unknown rank subcommand. Try /gog help");
        return GoGInput.done();
    }

    // -------------------------
    // rank create/delete/set/get
    // -------------------------

    private CompletableFuture<Void> handleRankCreate(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.rank.create")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.rank.create");
            return GoGInput.done();
        }

        if (args.length < 3) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog rank create <name> [weight]");
            return GoGInput.done();
        }

        String name = args[2].toLowerCase(Locale.ROOT);
        int weight = 0;
        if (args.length >= 4) weight = (int) GoGInput.parseLong(args[3], 0);

        boolean ok = stores.permManager().createGroup(name, weight);
        if (!ok) {
            GoGInput.send(ctx, "&c[GoG] Could not create rank: &f" + name);
            return GoGInput.done();
        }

        GoGInput.send(ctx, "&a[GoG] Created rank &f" + name + " &aweight=&f" + weight);
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleRankDelete(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.rank.delete")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.rank.delete");
            return GoGInput.done();
        }

        if (args.length < 3) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog rank delete <name>");
            return GoGInput.done();
        }

        GoGInput.send(ctx, "&c[GoG] Rank deletion is not supported in this build yet.");
        GoGInput.send(ctx, "&7[GoG] Add a deleteGroup method to PermissionManager if you want this enabled.");
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleRankSet(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.rank.set")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.rank.set");
            return GoGInput.done();
        }

        if (args.length < 4) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog rank set <playerName> <rank>");
            return GoGInput.done();
        }

        PlayerRef target = GoGPlayers.findPlayerRefByName(args[2]);
        if (target == null) {
            GoGInput.send(ctx, "&c[GoG] Player not found: &f" + args[2]);
            return GoGInput.done();
        }

        String group = args[3].toLowerCase(Locale.ROOT);

        PermissionManager pm = stores.permManager();
        if (pm.getGroup(group) == null) {
            GoGInput.send(ctx, "&c[GoG] Rank not found: &f" + group);
            return GoGInput.done();
        }

        boolean ok = pm.setUserGroup(target.getUuid(), group);
        if (!ok) {
            GoGInput.send(ctx, "&c[GoG] Failed to set rank.");
            return GoGInput.done();
        }

        GoGInput.send(ctx, "&a[GoG] Set &f" + target.getUsername() + " &arank to &f" + group);
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleRankGet(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.rank.get")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.rank.get");
            return GoGInput.done();
        }

        UUID tid = uuid;
        String label = "you";

        if (args.length >= 3) {
            PlayerRef target = GoGPlayers.findPlayerRefByName(args[2]);
            if (target == null) {
                GoGInput.send(ctx, "&c[GoG] Player not found: &f" + args[2]);
                return GoGInput.done();
            }
            tid = target.getUuid();
            label = target.getUsername() == null ? args[2] : target.getUsername();
        }

        String group = stores.permManager().getPrimaryGroup(tid);
        GoGInput.send(ctx, "&6[GoG] Rank for &f" + label + "&6: &f" + (group == null ? "default" : group));
        return GoGInput.done();
    }

    // -------------------------
    // rank perm add/remove
    // -------------------------

    private CompletableFuture<Void> handleRankPerm(CommandContext ctx, String[] args) {
        if (args.length < 5) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog rank perm add|remove <rank> <node>");
            return GoGInput.done();
        }

        if (GoGInput.eq(args[2], "add")) return handleRankPermAdd(ctx, args);
        if (GoGInput.eq(args[2], "remove")) return handleRankPermRemove(ctx, args);

        GoGInput.send(ctx, "&c[GoG] Usage: /gog rank perm add|remove <rank> <node>");
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleRankPermAdd(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.rank.perm.add")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.rank.perm.add");
            return GoGInput.done();
        }

        String group = args[3].toLowerCase(Locale.ROOT);
        String node = args[4];

        PermissionManager pm = stores.permManager();
        if (pm.getGroup(group) == null) {
            GoGInput.send(ctx, "&c[GoG] Rank not found: &f" + group);
            return GoGInput.done();
        }

        pm.groupAddPermissions(group, Collections.singleton(node));
        GoGInput.send(ctx, "&a[GoG] Added &f" + node + " &ato rank &f" + group);
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleRankPermRemove(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.rank.perm.remove")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.rank.perm.remove");
            return GoGInput.done();
        }

        String group = args[3].toLowerCase(Locale.ROOT);
        String node = args[4];

        PermissionManager pm = stores.permManager();
        if (pm.getGroup(group) == null) {
            GoGInput.send(ctx, "&c[GoG] Rank not found: &f" + group);
            return GoGInput.done();
        }

        pm.groupRemovePermissions(group, Collections.singleton(node));
        GoGInput.send(ctx, "&a[GoG] Removed &f" + node + " &afrom rank &f" + group);
        return GoGInput.done();
    }

    // -------------------------
    // rank prefix set/get/clear
    // -------------------------

    private CompletableFuture<Void> handleRankPrefix(CommandContext ctx, String[] args) {
        if (args.length < 4) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog rank prefix set|get|clear <rank> [prefix...]");
            return GoGInput.done();
        }

        if (GoGInput.eq(args[2], "set")) return handleRankPrefixSet(ctx, args);
        if (GoGInput.eq(args[2], "get")) return handleRankPrefixGet(ctx, args);
        if (GoGInput.eq(args[2], "clear")) return handleRankPrefixClear(ctx, args);

        GoGInput.send(ctx, "&c[GoG] Usage: /gog rank prefix set|get|clear <rank> [prefix...]");
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleRankPrefixSet(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.rank.prefix.set")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.rank.prefix.set");
            return GoGInput.done();
        }

        if (args.length < 5) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog rank prefix set <rank> <prefix...>");
            GoGInput.send(ctx, "&7[GoG] Example: &f/gog rank prefix set default &c[Default]&r");
            return GoGInput.done();
        }

        String groupName = args[3].toLowerCase(Locale.ROOT);

        PermissionManager pm = stores.permManager();
        Group g = pm.getGroup(groupName);
        if (g == null) {
            GoGInput.send(ctx, "&c[GoG] Rank not found: &f" + groupName);
            return GoGInput.done();
        }

        String raw = GoGInput.getInputString(ctx);
        if (raw == null) raw = "";

        String prefix = GoGInput.extractPrefixFromRaw(raw, groupName);
        if (prefix == null || prefix.isBlank()) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog rank prefix set <rank> <prefix...>");
            return GoGInput.done();
        }

        try {
            if (g.getMeta() == null) g.setMeta(new Meta());
            g.getMeta().setPrefix(prefix);
            pm.saveData();
        } catch (Throwable t) {
            GoGInput.send(ctx, "&c[GoG] Failed to set prefix due to API mismatch.");
            return GoGInput.done();
        }

        GoGInput.send(ctx, "&a[GoG] Set prefix for &f" + groupName + " &ato:");
        ctx.sender().sendMessage(MessageUtil.legacy(prefix + "&r"));
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleRankPrefixGet(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.rank.prefix.get")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.rank.prefix.get");
            return GoGInput.done();
        }

        if (args.length < 4) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog rank prefix get <rank>");
            return GoGInput.done();
        }

        String groupName = args[3].toLowerCase(Locale.ROOT);

        PermissionManager pm = stores.permManager();
        Group g = pm.getGroup(groupName);
        if (g == null) {
            GoGInput.send(ctx, "&c[GoG] Rank not found: &f" + groupName);
            return GoGInput.done();
        }

        String prefix = "";
        try {
            prefix = (g.getMeta() == null) ? "" : GoGInput.safe(g.getMeta().getPrefix());
        } catch (Throwable ignored) {}

        GoGInput.send(ctx, "&6[GoG] Prefix for &f" + groupName + "&6:");
        if (prefix.isBlank()) {
            GoGInput.send(ctx, "&7(none)");
        } else {
            ctx.sender().sendMessage(MessageUtil.legacy(prefix + "&r &7" + groupName));
        }
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleRankPrefixClear(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.rank.prefix.clear")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.rank.prefix.clear");
            return GoGInput.done();
        }

        if (args.length < 4) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog rank prefix clear <rank>");
            return GoGInput.done();
        }

        String groupName = args[3].toLowerCase(Locale.ROOT);

        PermissionManager pm = stores.permManager();
        Group g = pm.getGroup(groupName);
        if (g == null) {
            GoGInput.send(ctx, "&c[GoG] Rank not found: &f" + groupName);
            return GoGInput.done();
        }

        try {
            if (g.getMeta() == null) g.setMeta(new Meta());
            g.getMeta().setPrefix("");
            pm.saveData();
        } catch (Throwable t) {
            GoGInput.send(ctx, "&c[GoG] Failed to clear prefix due to API mismatch.");
            return GoGInput.done();
        }

        GoGInput.send(ctx, "&a[GoG] Cleared prefix for &f" + groupName);
        return GoGInput.done();
    }

    // -------------------------
    // rank prefixcolor set/get/clear
    // -------------------------

    private CompletableFuture<Void> handleRankPrefixColor(CommandContext ctx, String[] args) {
        if (args.length < 4) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog rank prefixcolor set|get|clear <rank> [&code]");
            return GoGInput.done();
        }

        if (GoGInput.eq(args[2], "set")) return handleRankPrefixColorSet(ctx, args);
        if (GoGInput.eq(args[2], "get")) return handleRankPrefixColorGet(ctx, args);
        if (GoGInput.eq(args[2], "clear")) return handleRankPrefixColorClear(ctx, args);

        GoGInput.send(ctx, "&c[GoG] Usage: /gog rank prefixcolor set|get|clear <rank> [&code]");
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleRankPrefixColorSet(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.rank.prefixcolor.set")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.rank.prefixcolor.set");
            return GoGInput.done();
        }

        if (args.length < 5) {
            GoGInput.send(ctx, "&c[GoG] Usage: /gog rank prefixcolor set <rank> <&code>");
            GoGInput.send(ctx, "&7[GoG] Example: &f/gog rank prefixcolor set default &c");
            return GoGInput.done();
        }

        String groupName = args[3].toLowerCase(Locale.ROOT);
        String color = args[4].trim();

        if (!GoGInput.isLegacyColorCode(color)) {
            GoGInput.send(ctx, "&c[GoG] Invalid color. Use one of: &f&0 &1 &2 &3 &4 &5 &6 &7 &8 &9 &a &b &c &d &e &f");
            return GoGInput.done();
        }

        PermissionManager pm = stores.permManager();
        Group g = pm.getGroup(groupName);
        if (g == null) {
            GoGInput.send(ctx, "&c[GoG] Rank not found: &f" + groupName);
            return GoGInput.done();
        }

        Meta meta = g.getMeta();
        if (meta == null) {
            meta = new Meta();
            g.setMeta(meta);
        }

        meta.setPrefixColor(color);
        pm.saveData();

        GoGInput.send(ctx, "&a[GoG] Set prefix color for &f" + groupName + " &ato: &f" + color);
        return GoGInput.done();
    }

    private CompletableFuture<Void> handleRankPrefixColorGet(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.rank.prefixcolor.get")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.rank.prefixcolor.get");
            return GoGInput.done();
        }

        String groupName = args[3].toLowerCase(Locale.ROOT);

        PermissionManager pm = stores.permManager();
        Group g = pm.getGroup(groupName);
        if (g == null) {
            GoGInput.send(ctx, "&c[GoG] Rank not found: &f" + groupName);
            return GoGInput.done();
        }

        String c = "";
        if (g.getMeta() != null && g.getMeta().getPrefixColor() != null) {
            c = g.getMeta().getPrefixColor();
        }

        if (c == null || c.isBlank()) {
            GoGInput.send(ctx, "&6[GoG] Prefix color for &f" + groupName + "&6: &7(none)");
        } else {
            GoGInput.send(ctx, "&6[GoG] Prefix color for &f" + groupName + "&6: &f" + c);
            ctx.sender().sendMessage(MessageUtil.legacy(c + "[Preview]" + "&r"));
        }

        return GoGInput.done();
    }

    private CompletableFuture<Void> handleRankPrefixColorClear(CommandContext ctx, String[] args) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        if (!GoGPermissions.has(ctx, stores, uuid, "gog.commands.rank.prefixcolor.clear")) {
            GoGInput.send(ctx, "&c[GoG] No permission: &f gog.commands.rank.prefixcolor.clear");
            return GoGInput.done();
        }

        String groupName = args[3].toLowerCase(Locale.ROOT);

        PermissionManager pm = stores.permManager();
        Group g = pm.getGroup(groupName);
        if (g == null) {
            GoGInput.send(ctx, "&c[GoG] Rank not found: &f" + groupName);
            return GoGInput.done();
        }

        Meta meta = g.getMeta();
        if (meta == null) {
            meta = new Meta();
            g.setMeta(meta);
        }

        meta.setPrefixColor("");
        pm.saveData();

        GoGInput.send(ctx, "&a[GoG] Cleared prefix color for &f" + groupName);
        return GoGInput.done();
    }
}
