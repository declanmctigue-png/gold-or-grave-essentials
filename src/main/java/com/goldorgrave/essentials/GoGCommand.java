package com.goldorgrave.essentials;

import com.goldorgrave.essentials.model.HomeLocation;
import com.goldorgrave.essentials.model.Kit;
import com.goldorgrave.essentials.model.KitItem;
import com.goldorgrave.essentials.storage.Stores;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class GoGCommand extends AbstractCommand {

    private final Stores stores;

    public GoGCommand(Stores stores) {
        super("gog", "Gold or Grave Essentials");
        this.stores = stores;
        setAllowsExtraArguments(true);
    }

    @Override
    protected @Nullable CompletableFuture<Void> execute(@NotNull CommandContext ctx) {
        String[] args = getArgs(ctx);

        if (args.length == 0 || eq(args[0], "help")) {
            send(ctx, "[GoG] Commands");
            send(ctx, "[GoG] /gog whoami");
            send(ctx, "[GoG] /gog permdebug");
            send(ctx, "[GoG] /gog ownerme (TEMP)");
            send(ctx, "[GoG] /gog homes");
            send(ctx, "[GoG] /gog sethome <name>");
            send(ctx, "[GoG] /gog home <name>");
            send(ctx, "[GoG] /gog delhome <name>");
            send(ctx, "[GoG] /gog kits");
            send(ctx, "[GoG] /gog kit <name>");
            send(ctx, "[GoG] /gog kit create <name> [cooldownSeconds] [permissionNode]");
            send(ctx, "[GoG] /gog kit delete <name>");
            send(ctx, "[GoG] /gog kit give <name> [playerName]");
            send(ctx, "[GoG] /gog ranks");
            send(ctx, "[GoG] /gog rank create <name> [priority]");
            send(ctx, "[GoG] /gog rank delete <name>");
            send(ctx, "[GoG] /gog rank perm add <rank> <node>");
            send(ctx, "[GoG] /gog rank perm remove <rank> <node>");
            send(ctx, "[GoG] /gog rank set <playerName> <rank>");
            send(ctx, "[GoG] /gog rank get [playerName]");
            return done();
        }

        if (eq(args[0], "whoami")) {
            UUID uuid = getUuid(ctx.sender());
            if (uuid == null) send(ctx, "[GoG] Player only command");
            else send(ctx, "[GoG] Your UUID: " + uuid);
            return done();
        }

        if (eq(args[0], "permdebug")) return handlePermDebug(ctx);
        if (eq(args[0], "ownerme")) return handleOwnerMe(ctx);

        // ranks
        if (eq(args[0], "ranks")) return handleRanksList(ctx);
        if (eq(args[0], "rank")) {
            if (args.length < 2) {
                send(ctx, "[GoG] Usage: /gog rank <create|delete|perm|set|get> ...");
                return done();
            }
            if (eq(args[1], "create")) return handleRankCreate(ctx, args);
            if (eq(args[1], "delete")) return handleRankDelete(ctx, args);
            if (eq(args[1], "set")) return handleRankSet(ctx, args);
            if (eq(args[1], "get")) return handleRankGet(ctx, args);

            if (eq(args[1], "perm")) {
                if (args.length < 5) {
                    send(ctx, "[GoG] Usage: /gog rank perm add|remove <rank> <node>");
                    return done();
                }
                if (eq(args[2], "add")) return handleRankPermAdd(ctx, args);
                if (eq(args[2], "remove")) return handleRankPermRemove(ctx, args);
                send(ctx, "[GoG] Usage: /gog rank perm add|remove <rank> <node>");
                return done();
            }

            send(ctx, "[GoG] Unknown rank subcommand. Try /gog help");
            return done();
        }

        // homes
        if (eq(args[0], "homes")) return handleHomesList(ctx);
        if (eq(args[0], "sethome")) return handleSetHome(ctx, args);
        if (eq(args[0], "home")) return handleHome(ctx, args);
        if (eq(args[0], "delhome")) return handleDelHome(ctx, args);

        // kits
        if (eq(args[0], "kits")) return handleKitsList(ctx);
        if (eq(args[0], "kit")) {
            if (args.length < 2) {
                send(ctx, "[GoG] Usage: /gog kit <name>");
                send(ctx, "[GoG] Admin: /gog kit create|delete|give ...");
                return done();
            }
            if (eq(args[1], "create")) return handleKitCreate(ctx, args);
            if (eq(args[1], "delete")) return handleKitDelete(ctx, args);
            if (eq(args[1], "give")) return handleKitGive(ctx, args);
            return handleKitUse(ctx, args[1]);
        }

        send(ctx, "[GoG] Unknown subcommand. Try /gog help");
        return done();
    }

    // ------------------------------------------------------------
    // TEMP: self-assign owner
    // ------------------------------------------------------------

    private CompletableFuture<Void> handleOwnerMe(CommandContext ctx) {
        UUID uuid = requirePlayer(ctx);
        if (uuid == null) return done();

        if (stores.hasAnyAdmins()) {
            if (!stores.hasPerm(ctx.sender(), uuid, "gog.commands.ownerme")) {
                send(ctx, "[GoG] No permission: gog.commands.ownerme");
                return done();
            }
        }

        stores.assignOwner(uuid);
        send(ctx, "[GoG] You are now owner.");
        return done();
    }

    // ------------------------------------------------------------
    // Rank commands (unchanged except using PlayerRef)
    // ------------------------------------------------------------

    private CompletableFuture<Void> handleRanksList(CommandContext ctx) {
        UUID uuid = requirePlayer(ctx);
        if (uuid == null) return done();
        if (!stores.hasPerm(ctx.sender(), uuid, "gog.commands.ranks")) {
            send(ctx, "[GoG] No permission: gog.commands.ranks");
            return done();
        }
        Set<String> ranks = stores.ranks().listRanks();
        send(ctx, "[GoG] Ranks: " + String.join(", ", ranks));
        return done();
    }

    private CompletableFuture<Void> handleRankCreate(CommandContext ctx, String[] args) {
        UUID uuid = requirePlayer(ctx);
        if (uuid == null) return done();
        if (!stores.hasPerm(ctx.sender(), uuid, "gog.commands.rank.create")) {
            send(ctx, "[GoG] No permission: gog.commands.rank.create");
            return done();
        }

        if (args.length < 3) {
            send(ctx, "[GoG] Usage: /gog rank create <name> [priority]");
            return done();
        }

        String name = args[2];
        int priority = 0;
        if (args.length >= 4) priority = (int) parseLong(args[3], 0);

        boolean ok = stores.ranks().createRank(name, priority);
        if (!ok) {
            send(ctx, "[GoG] Could not create rank (already exists or invalid): " + name);
            return done();
        }

        send(ctx, "[GoG] Created rank " + name + " priority=" + priority);
        return done();
    }

    private CompletableFuture<Void> handleRankDelete(CommandContext ctx, String[] args) {
        UUID uuid = requirePlayer(ctx);
        if (uuid == null) return done();
        if (!stores.hasPerm(ctx.sender(), uuid, "gog.commands.rank.delete")) {
            send(ctx, "[GoG] No permission: gog.commands.rank.delete");
            return done();
        }

        if (args.length < 3) {
            send(ctx, "[GoG] Usage: /gog rank delete <name>");
            return done();
        }

        String name = args[2];
        boolean ok = stores.ranks().deleteRank(name);
        if (!ok) {
            send(ctx, "[GoG] Could not delete rank: " + name);
            return done();
        }

        send(ctx, "[GoG] Deleted rank " + name);
        return done();
    }

    private CompletableFuture<Void> handleRankPermAdd(CommandContext ctx, String[] args) {
        UUID uuid = requirePlayer(ctx);
        if (uuid == null) return done();
        if (!stores.hasPerm(ctx.sender(), uuid, "gog.commands.rank.perm.add")) {
            send(ctx, "[GoG] No permission: gog.commands.rank.perm.add");
            return done();
        }

        String rank = args[3];
        String node = args[4];

        boolean ok = stores.ranks().addPermToRank(rank, node);
        if (!ok) {
            send(ctx, "[GoG] Failed to add permission. Check rank name and node.");
            return done();
        }

        send(ctx, "[GoG] Added " + node + " to rank " + rank);
        return done();
    }

    private CompletableFuture<Void> handleRankPermRemove(CommandContext ctx, String[] args) {
        UUID uuid = requirePlayer(ctx);
        if (uuid == null) return done();
        if (!stores.hasPerm(ctx.sender(), uuid, "gog.commands.rank.perm.remove")) {
            send(ctx, "[GoG] No permission: gog.commands.rank.perm.remove");
            return done();
        }

        String rank = args[3];
        String node = args[4];

        boolean ok = stores.ranks().removePermFromRank(rank, node);
        if (!ok) {
            send(ctx, "[GoG] Failed to remove permission. Check rank name and node.");
            return done();
        }

        send(ctx, "[GoG] Removed " + node + " from rank " + rank);
        return done();
    }

    private CompletableFuture<Void> handleRankSet(CommandContext ctx, String[] args) {
        UUID uuid = requirePlayer(ctx);
        if (uuid == null) return done();
        if (!stores.hasPerm(ctx.sender(), uuid, "gog.commands.rank.set")) {
            send(ctx, "[GoG] No permission: gog.commands.rank.set");
            return done();
        }

        if (args.length < 4) {
            send(ctx, "[GoG] Usage: /gog rank set <playerName> <rank>");
            return done();
        }

        PlayerRef target = findPlayerRefByName(args[2]);
        if (target == null) {
            send(ctx, "[GoG] Player not found: " + args[2]);
            return done();
        }

        boolean ok = stores.ranks().setUserRank(target.getUuid(), args[3]);
        if (!ok) {
            send(ctx, "[GoG] Failed to set rank. Does rank exist? /gog ranks");
            return done();
        }

        send(ctx, "[GoG] Set " + target.getUsername() + " rank to " + args[3]);
        return done();
    }

    private CompletableFuture<Void> handleRankGet(CommandContext ctx, String[] args) {
        UUID uuid = requirePlayer(ctx);
        if (uuid == null) return done();
        if (!stores.hasPerm(ctx.sender(), uuid, "gog.commands.rank.get")) {
            send(ctx, "[GoG] No permission: gog.commands.rank.get");
            return done();
        }

        UUID tid = uuid;
        String label = "you";

        if (args.length >= 3) {
            PlayerRef target = findPlayerRefByName(args[2]);
            if (target == null) {
                send(ctx, "[GoG] Player not found: " + args[2]);
                return done();
            }
            tid = target.getUuid();
            label = target.getUsername() == null ? args[2] : target.getUsername();
        }

        String rank = stores.ranks().getUserRank(tid);
        send(ctx, "[GoG] Rank for " + label + ": " + rank);
        return done();
    }

    // ------------------------------------------------------------
    // Permission debug (unchanged)
    // ------------------------------------------------------------

    private CompletableFuture<Void> handlePermDebug(CommandContext ctx) {
        Object sender = ctx.sender();

        send(ctx, "[GoG] senderClass=" + (sender == null ? "null" : sender.getClass().getName()));

        Object unwrapped = unwrapSender(sender);
        send(ctx, "[GoG] unwrappedClass=" + (unwrapped == null ? "null" : unwrapped.getClass().getName()));

        if (unwrapped != null) {
            String[] probes = new String[] {
                    "isOp", "isOperator", "isAdmin", "hasOperatorPermissions",
                    "getPermissionLevel", "getRole", "getRank", "getOperatorLevel"
            };

            for (String p : probes) {
                Object v = invoke0(unwrapped, p);
                if (v != null) send(ctx, "[GoG] " + p + "() -> " + v + " (" + v.getClass().getSimpleName() + ")");
            }

            int shown = 0;
            for (Method m : unwrapped.getClass().getMethods()) {
                if (shown >= 25) break;
                String n = m.getName().toLowerCase(Locale.ROOT);
                if (n.contains("op") || n.contains("admin") || n.contains("perm") || n.contains("role") || n.contains("rank")) {
                    send(ctx, "[GoG] method: " + m.getName() + "(" + m.getParameterCount() + ")");
                    shown++;
                }
            }
        }

        return done();
    }

    private static Object unwrapSender(Object sender) {
        if (sender == null) return null;

        Object v;
        v = invoke0(sender, "getPlayer");
        if (v != null) return v;
        v = invoke0(sender, "player");
        if (v != null) return v;

        v = invoke0(sender, "getEntity");
        if (v != null) return v;
        v = invoke0(sender, "entity");
        if (v != null) return v;

        return sender;
    }

    private static Object invoke0(Object obj, String method) {
        try {
            Method m = obj.getClass().getMethod(method);
            m.setAccessible(true);
            return m.invoke(obj);
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ------------------------------------------------------------
    // Homes (WORKING: TransformComponent + Teleport component)
    // ------------------------------------------------------------

    private CompletableFuture<Void> handleHomesList(CommandContext ctx) {
        UUID uuid = requirePlayer(ctx);
        if (uuid == null) return done();

        if (!stores.hasPerm(ctx.sender(), uuid, "gog.commands.homes")) {
            send(ctx, "[GoG] No permission: gog.commands.homes");
            return done();
        }

        Set<String> homes = stores.listHomes(uuid);
        if (homes.isEmpty()) {
            send(ctx, "[GoG] You have no homes set");
            return done();
        }

        send(ctx, "[GoG] Homes: " + String.join(", ", homes));
        return done();
    }

    private CompletableFuture<Void> handleSetHome(CommandContext ctx, String[] args) {
        UUID uuid = requirePlayer(ctx);
        if (uuid == null) return done();

        if (!stores.hasPerm(ctx.sender(), uuid, "gog.commands.sethome")) {
            send(ctx, "[GoG] No permission: gog.commands.sethome");
            return done();
        }

        if (args.length < 2) {
            send(ctx, "[GoG] Usage: /gog sethome <name>");
            return done();
        }

        String name = args[1].toLowerCase(Locale.ROOT);

        return captureHomeLocationFromEntityStore(ctx).thenCompose(loc -> {
            if (loc == null) {
                send(ctx, "[GoG] Could not get your position.");
                send(ctx, "[GoG] If you are in limbo between worlds, try again after loading in.");
                return done();
            }
            stores.setHome(uuid, name, loc);
            send(ctx, "[GoG] Set home " + name);
            return done();
        });
    }

    private CompletableFuture<Void> handleHome(CommandContext ctx, String[] args) {
        UUID uuid = requirePlayer(ctx);
        if (uuid == null) return done();

        if (!stores.hasPerm(ctx.sender(), uuid, "gog.commands.home")) {
            send(ctx, "[GoG] No permission: gog.commands.home");
            return done();
        }

        if (args.length < 2) {
            send(ctx, "[GoG] Usage: /gog home <name>");
            return done();
        }

        String name = args[1].toLowerCase(Locale.ROOT);
        HomeLocation loc = stores.getHome(uuid, name);
        if (loc == null) {
            send(ctx, "[GoG] Home not found: " + name);
            return done();
        }

        return teleportToHomeViaTeleportComponent(ctx, loc).thenCompose(ok -> {
            if (!ok) send(ctx, "[GoG] Teleport failed (check console logs for [GoG]).");
            else send(ctx, "[GoG] Teleported to home " + name);
            return done();
        });
    }

    private CompletableFuture<Void> handleDelHome(CommandContext ctx, String[] args) {
        UUID uuid = requirePlayer(ctx);
        if (uuid == null) return done();

        if (!stores.hasPerm(ctx.sender(), uuid, "gog.commands.delhome")) {
            send(ctx, "[GoG] No permission: gog.commands.delhome");
            return done();
        }

        if (args.length < 2) {
            send(ctx, "[GoG] Usage: /gog delhome <name>");
            return done();
        }

        String name = args[1].toLowerCase(Locale.ROOT);
        boolean ok = stores.delHome(uuid, name);
        if (!ok) send(ctx, "[GoG] Home not found: " + name);
        else send(ctx, "[GoG] Deleted home " + name);
        return done();
    }

    private CompletableFuture<HomeLocation> captureHomeLocationFromEntityStore(CommandContext ctx) {
        try {
            UUID uuid = getUuid(ctx.sender());
            if (uuid == null) return CompletableFuture.completedFuture(null);

            Universe u = Universe.get();
            if (u == null) return CompletableFuture.completedFuture(null);

            PlayerRef pr = u.getPlayer(uuid);
            if (pr == null) return CompletableFuture.completedFuture(null);

            UUID worldUuid = pr.getWorldUuid();
            if (worldUuid == null) return CompletableFuture.completedFuture(null);

            World world = u.getWorld(worldUuid);
            if (world == null) return CompletableFuture.completedFuture(null);

            Ref<EntityStore> ref = pr.getReference();
            if (ref == null) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            if (store == null) return CompletableFuture.completedFuture(null);

            CompletableFuture<HomeLocation> out = new CompletableFuture<>();

            world.execute(() -> {
                try {
                    TransformComponent tc = (TransformComponent) store.getComponent(ref, TransformComponent.getComponentType());
                    if (tc == null) {
                        out.complete(null);
                        return;
                    }

                    Vector3d pos = tc.getPosition();
                    if (pos == null) {
                        out.complete(null);
                        return;
                    }

                    String wname = world.getName() == null ? "default" : world.getName();
                    out.complete(new HomeLocation(wname, pos.getX(), pos.getY(), pos.getZ()));
                } catch (Throwable t) {
                    System.out.println("[GoG] captureHomeLocationFromEntityStore failed: " + t);
                    t.printStackTrace();
                    out.complete(null);
                }
            });

            return out;
        } catch (Throwable t) {
            System.out.println("[GoG] captureHomeLocationFromEntityStore outer failed: " + t);
            t.printStackTrace();
            return CompletableFuture.completedFuture(null);
        }
    }

    private CompletableFuture<Boolean> teleportToHomeViaTeleportComponent(CommandContext ctx, HomeLocation loc) {
        try {
            UUID uuid = getUuid(ctx.sender());
            if (uuid == null) return CompletableFuture.completedFuture(false);

            Universe u = Universe.get();
            if (u == null) return CompletableFuture.completedFuture(false);

            PlayerRef pr = u.getPlayer(uuid);
            if (pr == null) return CompletableFuture.completedFuture(false);

            Ref<EntityStore> ref = pr.getReference();
            if (ref == null) return CompletableFuture.completedFuture(false);

            Store<EntityStore> store = ref.getStore();
            if (store == null) return CompletableFuture.completedFuture(false);

            UUID currentWorldUuid = pr.getWorldUuid();
            if (currentWorldUuid == null) return CompletableFuture.completedFuture(false);

            World sourceWorld = u.getWorld(currentWorldUuid);
            if (sourceWorld == null) return CompletableFuture.completedFuture(false);

            // Best effort target world (fallback to same world if lookup fails)
            World targetWorld = (loc.world == null) ? null : u.getWorld(loc.world);
            if (targetWorld == null) targetWorld = sourceWorld;

            Vector3d destPos = new Vector3d(loc.x, loc.y, loc.z);

            CompletableFuture<Boolean> out = new CompletableFuture<>();
            World finalTargetWorld = targetWorld;

            sourceWorld.execute(() -> {
                try {
                    // Rotation must not be null
                    Vector3f rot = new Vector3f(0.0f, 0.0f, 0.0f);

                    // If available, use the player's current head rotation
                    HeadRotation hr = (HeadRotation) store.getComponent(ref, HeadRotation.getComponentType());
                    if (hr != null && hr.getRotation() != null) {
                        rot = hr.getRotation();
                    }

                    Teleport tp = new Teleport(finalTargetWorld, destPos, rot).withHeadRotation(rot);
                    store.addComponent(ref, Teleport.getComponentType(), tp);

                    out.complete(true);
                } catch (Throwable t) {
                    System.out.println("[GoG] teleportToHomeViaTeleportComponent failed: " + t);
                    t.printStackTrace();
                    out.complete(false);
                }
            });

            return out;

        } catch (Throwable t) {
            System.out.println("[GoG] teleportToHomeViaTeleportComponent outer failed: " + t);
            t.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }


    // ------------------------------------------------------------
    // Kits (your existing code below)
    // ------------------------------------------------------------

    private CompletableFuture<Void> handleKitsList(CommandContext ctx) {
        UUID uuid = requirePlayer(ctx);
        if (uuid == null) return done();

        if (!stores.hasPerm(ctx.sender(), uuid, "gog.commands.kits.list")) {
            send(ctx, "[GoG] No permission: gog.commands.kits.list");
            return done();
        }

        Set<String> kits = stores.listKits();
        if (kits.isEmpty()) {
            send(ctx, "[GoG] No kits exist");
            return done();
        }

        send(ctx, "[GoG] Kits: " + String.join(", ", kits));
        send(ctx, "[GoG] Use: /gog kit <name>");
        return done();
    }

    private CompletableFuture<Void> handleKitCreate(CommandContext ctx, String[] args) {
        UUID uuid = requirePlayer(ctx);
        if (uuid == null) return done();

        if (!stores.hasPerm(ctx.sender(), uuid, "gog.commands.kit.create")) {
            send(ctx, "[GoG] No permission: gog.commands.kit.create");
            return done();
        }

        if (args.length < 3) {
            send(ctx, "[GoG] Usage: /gog kit create <name> [cooldownSeconds] [permissionNode]");
            return done();
        }

        String kitName = args[2];
        long cooldownSeconds = 0;
        if (args.length >= 4) cooldownSeconds = parseLong(args[3], 0);

        String permissionNode = "gog.kit." + kitName.toLowerCase(Locale.ROOT);
        if (args.length >= 5) permissionNode = args[4];

        List<KitItem> items = snapshotInventoryAsKitItems(ctx.sender());
        if (items == null || items.isEmpty()) {
            send(ctx, "[GoG] Could not read inventory or inventory empty");
            return done();
        }

        Kit kit = new Kit(kitName, permissionNode, cooldownSeconds, items);
        stores.upsertKit(kit);

        send(ctx, "[GoG] Saved kit " + kitName + " with " + items.size() + " items");
        return done();
    }

    private CompletableFuture<Void> handleKitDelete(CommandContext ctx, String[] args) {
        UUID uuid = requirePlayer(ctx);
        if (uuid == null) return done();

        if (!stores.hasPerm(ctx.sender(), uuid, "gog.commands.kit.delete")) {
            send(ctx, "[GoG] No permission: gog.commands.kit.delete");
            return done();
        }

        if (args.length < 3) {
            send(ctx, "[GoG] Usage: /gog kit delete <name>");
            return done();
        }

        boolean ok = stores.deleteKit(args[2]);
        if (!ok) send(ctx, "[GoG] Kit not found: " + args[2]);
        else send(ctx, "[GoG] Deleted kit " + args[2]);
        return done();
    }

    private CompletableFuture<Void> handleKitGive(CommandContext ctx, String[] args) {
        UUID uuid = requirePlayer(ctx);
        if (uuid == null) return done();

        if (!stores.hasPerm(ctx.sender(), uuid, "gog.commands.kit.give")) {
            send(ctx, "[GoG] No permission: gog.commands.kit.give");
            return done();
        }

        if (args.length < 3) {
            send(ctx, "[GoG] Usage: /gog kit give <kit> [playerName]");
            return done();
        }

        Kit kit = stores.getKit(args[2]);
        if (kit == null) {
            send(ctx, "[GoG] Kit not found: " + args[2]);
            return done();
        }

        Object target = ctx.sender();
        if (args.length >= 4) {
            Object found = findPlayerEntityByName(ctx, args[3]);
            if (found == null) {
                send(ctx, "[GoG] Player not found: " + args[3]);
                return done();
            }
            target = found;
        }

        boolean ok = giveKitToPlayer(target, kit);
        if (!ok) {
            send(ctx, "[GoG] Failed to give kit");
            return done();
        }

        send(ctx, "[GoG] Gave kit " + kit.name());
        return done();
    }

    private CompletableFuture<Void> handleKitUse(CommandContext ctx, String kitName) {
        UUID uuid = requirePlayer(ctx);
        if (uuid == null) return done();

        Kit kit = stores.getKit(kitName);
        if (kit == null) {
            send(ctx, "[GoG] Kit not found: " + kitName);
            return done();
        }

        String perm = kit.permission();
        if (perm != null && !perm.isBlank() && !stores.hasPerm(ctx.sender(), uuid, perm)) {
            send(ctx, "[GoG] No permission: " + perm);
            return done();
        }

        long now = System.currentTimeMillis();
        long nextOk = stores.nextKitAllowed(uuid, kit.name());
        if (nextOk > now) {
            long seconds = Math.max(1, (nextOk - now) / 1000);
            send(ctx, "[GoG] You can use that kit again in " + seconds + " seconds");
            return done();
        }

        boolean ok = giveKitToPlayer(ctx.sender(), kit);
        if (!ok) {
            send(ctx, "[GoG] Failed to give kit");
            return done();
        }

        long cd = kit.cooldownSeconds();
        if (cd > 0) stores.setKitCooldown(uuid, kit.name(), now + (cd * 1000L));

        send(ctx, "[GoG] Kit received: " + kit.name());
        return done();
    }

    private List<KitItem> snapshotInventoryAsKitItems(Object sender) {
        try {
            LivingEntity entity = asLivingEntity(sender);
            if (entity == null) return null;

            Inventory inv = entity.getInventory();
            if (inv == null) return null;

            ItemContainer hotbar = inv.getHotbar();
            ItemContainer storage = inv.getStorage();

            Map<String, Integer> counts = new LinkedHashMap<>();
            if (hotbar != null) hotbar.forEach((slot, stack) -> accumulateKitStack(counts, stack));
            if (storage != null) storage.forEach((slot, stack) -> accumulateKitStack(counts, stack));

            List<KitItem> out = new ArrayList<>();
            for (Map.Entry<String, Integer> e : counts.entrySet()) out.add(new KitItem(e.getKey(), e.getValue()));
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void accumulateKitStack(Map<String, Integer> counts, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        String id = stack.getItemId();
        int qty = stack.getQuantity();
        if (id == null || id.isBlank() || qty <= 0) return;
        counts.put(id, counts.getOrDefault(id, 0) + qty);
    }

    private boolean giveKitToPlayer(Object targetSender, Kit kit) {
        try {
            LivingEntity entity = asLivingEntity(targetSender);
            if (entity == null) return false;

            Inventory inv = entity.getInventory();
            if (inv == null) return false;

            CombinedItemContainer combined = inv.getCombinedHotbarFirst();

            for (KitItem item : kit.items()) {
                if (item == null) continue;
                String id = item.itemId();
                int amount = item.amount();
                if (id == null || id.isBlank() || amount <= 0) continue;

                ItemStack stack = new ItemStack(id, amount);

                ItemStackTransaction tx = (combined != null)
                        ? combined.addItemStack(stack)
                        : inv.getStorage().addItemStack(stack);

                ItemStack remainder = tx.getRemainder();
                if (remainder != null && !remainder.isEmpty() && remainder.getQuantity() > 0) {
                    System.out.println("[GoG] giveKitToPlayer: not enough space for " + id + " x" + amount
                            + " remainder=" + remainder.getQuantity());
                }
            }

            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private LivingEntity asLivingEntity(Object sender) {
        if (sender == null) return null;
        if (sender instanceof LivingEntity le) return le;

        Object v = invoke(sender, "getPlayer");
        if (v instanceof LivingEntity le) return le;
        v = invoke(sender, "player");
        if (v instanceof LivingEntity le) return le;

        v = invoke(sender, "getEntity");
        if (v instanceof LivingEntity le) return le;
        v = invoke(sender, "entity");
        if (v instanceof LivingEntity le) return le;

        return null;
    }

    // ------------------------------------------------------------
    // Player helpers
    // ------------------------------------------------------------

    private UUID requirePlayer(CommandContext ctx) {
        UUID uuid = getUuid(ctx.sender());
        if (uuid == null) {
            send(ctx, "[GoG] Player only command");
            return null;
        }
        return uuid;
    }

    private static UUID getUuid(Object sender) {
        if (sender == null) return null;
        try {
            Method m = sender.getClass().getMethod("getUuid");
            return (UUID) m.invoke(sender);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private PlayerRef findPlayerRefByName(String username) {
        if (username == null || username.isBlank()) return null;

        try {
            Universe u = Universe.get();
            if (u == null) return null;

            PlayerRef pr = u.getPlayerByUsername(username, NameMatching.STARTS_WITH_IGNORE_CASE);
            if (pr == null) pr = u.getPlayerByUsername(username, NameMatching.EXACT_IGNORE_CASE);
            return pr;
        } catch (Throwable t) {
            return null;
        }
    }

    private Object findPlayerEntityByName(CommandContext ctx, String name) {
        try {
            Object server = invoke(ctx, "getServer");
            if (server == null) server = invoke(ctx, "server");
            if (server == null) return null;

            Object pm = invoke(server, "getPlayerManager");
            if (pm == null) pm = invoke(server, "playerManager");
            if (pm == null) return null;

            Object p = invoke(pm, "getPlayerByName", name);
            if (p == null) p = invoke(pm, "findByName", name);
            if (p == null) p = invoke(pm, "getPlayer", name);
            return p;
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ------------------------------------------------------------
    // Input parsing
    // ------------------------------------------------------------

    private static String[] getArgs(CommandContext ctx) {
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

    private static String getInputString(CommandContext ctx) {
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

    // ------------------------------------------------------------
    // Reflection helpers
    // ------------------------------------------------------------

    private static Object invoke(Object obj, String method, Object... args) {
        if (obj == null) return null;
        try {
            Method m = findMethod(obj.getClass(), method, args);
            if (m == null) return null;
            m.setAccessible(true);
            return m.invoke(obj, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> cls, String name, Object... args) {
        Method[] methods = cls.getMethods();
        outer:
        for (Method m : methods) {
            if (!m.getName().equals(name)) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length != args.length) continue;
            for (int i = 0; i < p.length; i++) {
                if (args[i] == null) continue;
                if (!box(p[i]).isAssignableFrom(box(args[i].getClass()))) continue outer;
            }
            return m;
        }
        return null;
    }

    private static Class<?> box(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == double.class) return Double.class;
        if (c == float.class) return Float.class;
        if (c == boolean.class) return Boolean.class;
        if (c == short.class) return Short.class;
        if (c == byte.class) return Byte.class;
        if (c == char.class) return Character.class;
        return c;
    }

    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s); } catch (Throwable e) { return def; }
    }

    private static boolean eq(String a, String b) {
        return a != null && a.equalsIgnoreCase(b);
    }

    private static void send(CommandContext ctx, String text) {
        ctx.sender().sendMessage(Message.raw(text));
    }

    private static CompletableFuture<Void> done() {
        return CompletableFuture.completedFuture(null);
    }
}
