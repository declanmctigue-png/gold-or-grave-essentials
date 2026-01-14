// File: com/goldorgrave/essentials/storage/Stores.java
package com.goldorgrave.essentials.storage;

import com.goldorgrave.essentials.model.HomeLocation;
import com.goldorgrave.essentials.model.Kit;
import com.goldorgrave.essentials.model.UserRecord;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Stores {

    // Bootstrapped super-rank for bypassing everything until you build full role tooling.
    private static final String OWNER_RANK = "owner";

    private final Path dataDir;

    // Basic in-memory storage (persist later)
    private final Map<UUID, UserRecord> users = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, HomeLocation>> homesByPlayer = new ConcurrentHashMap<>();
    private final Map<String, Kit> kitsByName = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> kitCooldownsByPlayer = new ConcurrentHashMap<>();

    private final RankService ranks = new RankService();

    public Stores(Path dataDir) {
        this.dataDir = dataDir;
    }

    public RankService ranks() {
        return ranks;
    }

    // ------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------

    public void ensureDefaults() {
        try {
            Files.createDirectories(dataDir);
        } catch (Exception ignored) {}

        // Rank defaults (default/admin)
        ranks.ensureDefaults();

        // Ensure owner rank exists with wildcard "*"
        if (ranks.getRank(OWNER_RANK) == null) {
            ranks.createRank(OWNER_RANK, 1000);
            ranks.addPermToRank(OWNER_RANK, "*");
            System.out.println("[GoG] Created owner rank with wildcard permission");
        }
    }

    public void saveAll() {
        // Persist later (json, etc.)
    }

    // ------------------------------------------------------------
    // Permission system (TEMP)
    // ------------------------------------------------------------

    public boolean hasPerm(Object sender, UUID uuid, String perm) {
        if (perm == null || perm.isBlank()) return true;
        if (uuid == null) return false;

        ensureUserExists(uuid);

        // 1) If server exposes operator, treat op as bypass (best-effort)
        //    Your earlier permdebug showed sender is Player, but op methods might not exist.
        Object unwrapped = unwrapSender(sender);
        if (isOperator(unwrapped)) return true;

        // 2) Bootstrap: if NO admins exist yet, first real player becomes owner automatically.
        //    This prevents locking yourself out during early development.
        if (!hasAnyAdmins()) {
            assignOwner(uuid);
            System.out.println("[GoG] Bootstrapped first admin/owner: " + uuid);
            return true;
        }

        // 3) If this user is owner in RankService, allow everything.
        String r = ranks.getUserRank(uuid);
        if (OWNER_RANK.equalsIgnoreCase(r)) return true;

        // 4) Normal permission check through RankService
        return ranks.hasPermission(uuid, perm);
    }

    public boolean hasAnyAdmins() {
        // If any user has OWNER_RANK in RankService mapping, we consider admins present.
        for (UUID id : users.keySet()) {
            String r = ranks.getUserRank(id);
            if (r != null && OWNER_RANK.equalsIgnoreCase(r)) return true;
        }
        return false;
    }

    public void assignOwner(UUID uuid) {
        if (uuid == null) return;
        ensureUserExists(uuid);
        ranks.setUserRank(uuid, OWNER_RANK);

        // Also keep the local user record ranks set in sync (optional, but helpful).
        UserRecord u = users.get(uuid);
        if (u != null) {
            u.ranks().add(OWNER_RANK);
        }
    }

    private void ensureUserExists(UUID uuid) {
        users.computeIfAbsent(uuid, UserRecord::new);
    }

    // ------------------------------------------------------------
    // Homes
    // ------------------------------------------------------------

    public Set<String> listHomes(UUID uuid) {
        Map<String, HomeLocation> m = homesByPlayer.get(uuid);
        if (m == null) return Collections.emptySet();
        return new TreeSet<>(m.keySet());
    }

    public void setHome(UUID uuid, String name, HomeLocation loc) {
        if (uuid == null || name == null || name.isBlank() || loc == null) return;
        homesByPlayer.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(name.toLowerCase(Locale.ROOT), loc);
    }

    public HomeLocation getHome(UUID uuid, String name) {
        if (uuid == null || name == null) return null;
        Map<String, HomeLocation> m = homesByPlayer.get(uuid);
        if (m == null) return null;
        return m.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean delHome(UUID uuid, String name) {
        if (uuid == null || name == null) return false;
        Map<String, HomeLocation> m = homesByPlayer.get(uuid);
        if (m == null) return false;
        return m.remove(name.toLowerCase(Locale.ROOT)) != null;
    }

    // ------------------------------------------------------------
    // Kits
    // ------------------------------------------------------------

    public Set<String> listKits() {
        return new TreeSet<>(kitsByName.keySet());
    }

    public Kit getKit(String name) {
        if (name == null) return null;
        return kitsByName.get(name.toLowerCase(Locale.ROOT));
    }

    public void upsertKit(Kit kit) {
        if (kit == null || kit.name() == null) return;
        kitsByName.put(kit.name().toLowerCase(Locale.ROOT), kit);
    }

    public boolean deleteKit(String name) {
        if (name == null) return false;
        return kitsByName.remove(name.toLowerCase(Locale.ROOT)) != null;
    }

    // ------------------------------------------------------------
    // Cooldowns
    // ------------------------------------------------------------

    public long nextKitAllowed(UUID uuid, String kitName) {
        if (uuid == null || kitName == null) return 0L;
        Map<String, Long> m = kitCooldownsByPlayer.get(uuid);
        if (m == null) return 0L;
        return m.getOrDefault(kitName.toLowerCase(Locale.ROOT), 0L);
    }

    public void setKitCooldown(UUID uuid, String kitName, long nextAllowedMs) {
        if (uuid == null || kitName == null) return;
        kitCooldownsByPlayer.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(kitName.toLowerCase(Locale.ROOT), nextAllowedMs);
    }

    // ------------------------------------------------------------
    // Sender unwrap + OP detection (best-effort)
    // ------------------------------------------------------------

    private static Object unwrapSender(Object sender) {
        if (sender == null) return null;

        Object v;
        v = invoke(sender, "getPlayer");
        if (v != null) return v;
        v = invoke(sender, "player");
        if (v != null) return v;

        v = invoke(sender, "getEntity");
        if (v != null) return v;
        v = invoke(sender, "entity");
        if (v != null) return v;

        return sender;
    }

    private static boolean isOperator(Object sender) {
        if (sender == null) return false;

        // These may or may not exist in your build. We try several.
        Boolean b;
        b = asBool(invoke(sender, "isOp"));
        if (b != null) return b;

        b = asBool(invoke(sender, "isOperator"));
        if (b != null) return b;

        b = asBool(invoke(sender, "isAdmin"));
        if (b != null) return b;

        b = asBool(invoke(sender, "hasOperatorPermissions"));
        if (b != null) return b;

        // If your docs reveal a different method name, add it here.
        return false;
    }

    private static Boolean asBool(Object o) {
        return (o instanceof Boolean b) ? b : null;
    }

    private static Object invoke(Object obj, String method) {
        try {
            Method m = obj.getClass().getMethod(method);
            m.setAccessible(true);
            return m.invoke(obj);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
