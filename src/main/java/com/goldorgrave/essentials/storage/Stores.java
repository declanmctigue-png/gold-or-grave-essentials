// File: com/goldorgrave/essentials/storage/Stores.java
package com.goldorgrave.essentials.storage;

import com.goldorgrave.essentials.perms.model.HomeLocation;
import com.goldorgrave.essentials.perms.model.Kit;
import com.goldorgrave.essentials.perms.model.UserRecord;
import com.goldorgrave.essentials.perms.PermissionManager;
import com.goldorgrave.essentials.perms.models.ChatMeta;
import com.goldorgrave.essentials.perms.models.Group;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Stores {

    public static final String OWNER_GROUP = "owner";

    private final Path dataDir;
    private final PermissionManager permManager;

    private final Map<UUID, UserRecord> users = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, HomeLocation>> homesByPlayer = new ConcurrentHashMap<>();
    private final Map<String, Kit> kitsByName = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> kitCooldownsByPlayer = new ConcurrentHashMap<>();

    private volatile String joinMessage = null;

    private final Perms perms = new Perms();

    // ------------------------------------------------------------
    // Join broadcast persistence
    // ------------------------------------------------------------

    private static final String JOIN_BROADCAST_FILE = "joinbroadcast.txt";
    private volatile String joinBroadcastMessage = "";

    public Stores(Path dataDir, PermissionManager permManager) {
        this.dataDir = dataDir;
        this.permManager = permManager;
    }

    public PermissionManager permManager() {
        return permManager;
    }

    // ------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------

    public void ensureDefaults() {
        try {
            Files.createDirectories(dataDir);
        } catch (Exception ignored) { }

        // Make sure perms data is loaded so listGroups has content
        permManager.loadData();

        // Ensure default group
        if (!permManager.listGroups().contains("default")) {
            permManager.createGroup("default", 0);
        }

        // Ensure owner group
        if (!permManager.listGroups().contains(OWNER_GROUP)) {
            permManager.createGroup(OWNER_GROUP, 1000);
            permManager.groupAddPermissions(OWNER_GROUP, Set.of("*"));

            // Do NOT hardcode prefix or colors here.
            // Leave everything blank so you control it via /gog rank prefix ... and /gog rank prefixcolor ...
            permManager.saveData();

            System.out.println("[GoG] Created owner group with wildcard permission");
        }

        // Load join broadcast template from disk
        loadJoinBroadcastMessage();

        ensureUsersFromUniverseBestEffort();
    }

    public void saveAll() {
        permManager.saveData();

        // Optional but safe: persist join broadcast on shutdown too
        saveJoinBroadcastMessage();
    }

    private void ensureUsersFromUniverseBestEffort() {
        try {
            Universe u = Universe.get();
            if (u == null) return;
            for (PlayerRef pr : u.getPlayers()) {
                if (pr == null) continue;
                UUID id = pr.getUuid();
                if (id == null) continue;
                users.computeIfAbsent(id, UserRecord::new);
            }
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------
    // Join message
    // ------------------------------------------------------------

    public void setJoinMessage(String msg) {
        if (msg != null && msg.isBlank()) msg = null;
        this.joinMessage = msg;
    }

    public String getJoinMessage() {
        return joinMessage;
    }

    // ------------------------------------------------------------
    // Join broadcast (persisted)
    // ------------------------------------------------------------

    public String getJoinBroadcastMessage() {
        String v = joinBroadcastMessage;
        if (v == null || v.isBlank()) return null;
        return v;
    }

    public void setJoinBroadcastMessage(String msg) {
        if (msg == null) msg = "";
        msg = msg.trim();
        this.joinBroadcastMessage = msg;
        saveJoinBroadcastMessage();
    }

    private Path joinBroadcastPath() {
        return dataDir.resolve(JOIN_BROADCAST_FILE);
    }

    private void loadJoinBroadcastMessage() {
        try {
            Path p = joinBroadcastPath();
            if (!Files.exists(p)) {
                joinBroadcastMessage = "";
                return;
            }
            String s = Files.readString(p, StandardCharsets.UTF_8);
            if (s == null) s = "";
            s = s.trim();
            joinBroadcastMessage = s;
            if (!s.isBlank()) {
                System.out.println("[GoG] Loaded join broadcast template from " + p);
            }
        } catch (Throwable t) {
            joinBroadcastMessage = "";
            System.out.println("[GoG] Failed to load join broadcast template: " + t);
        }
    }

    private void saveJoinBroadcastMessage() {
        try {
            Files.createDirectories(dataDir);
            Path p = joinBroadcastPath();

            String s = joinBroadcastMessage;
            if (s == null) s = "";
            s = s.trim();

            if (s.isBlank()) {
                // Clearing means delete file
                try {
                    Files.deleteIfExists(p);
                } catch (Throwable ignored) {}
                return;
            }

            Files.writeString(p, s, StandardCharsets.UTF_8);
        } catch (Throwable t) {
            System.out.println("[GoG] Failed to save join broadcast template: " + t);
        }
    }

    // ------------------------------------------------------------
    // Permissions surface used by commands and listeners
    // ------------------------------------------------------------

    public Perms perms() {
        return perms;
    }

    public final class Perms {

        public boolean has(Object sender, UUID uuid, String perm) {
            if (perm == null || perm.isBlank()) return true;
            if (uuid == null) return false;

            ensureUserExists(uuid);

            permManager.ensureUserLoaded(uuid, tryUsername(sender));

            Object unwrapped = unwrapSender(sender);
            if (isOperator(unwrapped)) return true;

            if (!hasAnyAdmins()) {
                assignOwner(uuid);
                System.out.println("[GoG] Bootstrapped first admin owner: " + uuid);
                return true;
            }

            String g = permManager.getPrimaryGroup(uuid);
            if (g != null && OWNER_GROUP.equalsIgnoreCase(g)) return true;

            return permManager.hasPermission(uuid, perm);
        }

        public Set<String> listGroups() {
            return permManager.listGroups();
        }

        public boolean createGroup(String name, int weight) {
            boolean ok = permManager.createGroup(name, weight);
            if (ok) permManager.saveData();
            return ok;
        }

        public void addPermission(String group, String permission) {
            if (group == null || permission == null) return;
            permManager.groupAddPermissions(group, Set.of(permission));
            permManager.saveData();
        }

        public void removePermission(String group, String permission) {
            if (group == null || permission == null) return;
            permManager.groupRemovePermissions(group, Set.of(permission));
            permManager.saveData();
        }

        public Set<String> listGroupPermissions(String group) {
            return permManager.groupListPermissions(group);
        }

        public boolean setPrimaryGroup(UUID uuid, String group) {
            boolean ok = permManager.setUserGroup(uuid, group);
            if (ok) permManager.saveData();
            return ok;
        }

        public String getPrimaryGroup(UUID uuid) {
            return permManager.getPrimaryGroup(uuid);
        }

        public void setPrefixMeta(String group, String prefix) {
            setGroupPrefix(group, prefix);
            permManager.saveData();
        }

        public String resolvePrefix(UUID uuid) {
            ChatMeta meta = permManager.getChatMeta(uuid);
            if (meta == null) return "";

            String prefix = meta.getPrefix();
            if (prefix == null || prefix.isBlank()) return "";

            String color = meta.getPrefixColor();
            String fmt = meta.getPrefixFormat();

            String out = "";
            if (color != null && !color.isBlank()) out += color;
            if (fmt != null && !fmt.isBlank()) out += fmt;
            out += prefix;

            out += "&r";
            return out;
        }
    }

    public boolean hasPerm(Object sender, UUID uuid, String perm) {
        return perms().has(sender, uuid, perm);
    }

    // ------------------------------------------------------------
    // Admin bootstrap helpers
    // ------------------------------------------------------------

    public boolean hasAnyAdmins() {
        for (UUID id : users.keySet()) {
            String g = permManager.getPrimaryGroup(id);
            if (g != null && OWNER_GROUP.equalsIgnoreCase(g)) return true;
        }
        return false;
    }

    public void assignOwner(UUID uuid) {
        if (uuid == null) return;
        ensureUserExists(uuid);

        permManager.ensureUserLoaded(uuid, null);
        permManager.setUserGroup(uuid, OWNER_GROUP);
        permManager.saveData();

        UserRecord u = users.get(uuid);
        if (u != null) {
            u.ranks().clear();
            u.ranks().add(OWNER_GROUP);
        }
    }

    private void ensureUserExists(UUID uuid) {
        users.computeIfAbsent(uuid, UserRecord::new);
    }

    private void setGroupPrefix(String groupName, String prefix) {
        if (groupName == null || groupName.isBlank()) return;

        Group g = permManager.getGroup(groupName);
        if (g == null) {
            permManager.createGroup(groupName, 0);
            g = permManager.getGroup(groupName);
            if (g == null) return;
        }

        if (prefix == null) prefix = "";
        g.getMeta().setPrefix(prefix);
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
    // Sender unwrap and OP detection
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

        Boolean b;
        b = asBool(invoke(sender, "isOp"));
        if (b != null) return b;

        b = asBool(invoke(sender, "isOperator"));
        if (b != null) return b;

        b = asBool(invoke(sender, "isAdmin"));
        if (b != null) return b;

        b = asBool(invoke(sender, "hasOperatorPermissions"));
        if (b != null) return b;

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

    private static String tryUsername(Object sender) {
        if (sender == null) return null;
        try {
            Method m = sender.getClass().getMethod("getUsername");
            Object v = m.invoke(sender);
            return v instanceof String ? (String) v : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
