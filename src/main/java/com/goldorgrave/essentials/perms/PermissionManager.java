package com.goldorgrave.essentials.perms;

import com.goldorgrave.essentials.perms.data.PermsStorage;
import com.goldorgrave.essentials.perms.models.ChatMeta;
import com.goldorgrave.essentials.perms.models.Group;
import com.goldorgrave.essentials.perms.models.Meta;
import com.goldorgrave.essentials.perms.models.Node;
import com.goldorgrave.essentials.perms.models.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PermissionManager {

    private final PermsStorage storage;

    private final Map<String, Group> groups = new ConcurrentHashMap<>();
    private final Map<UUID, User> users = new ConcurrentHashMap<>();

    public PermissionManager(PermsStorage storage) {
        this.storage = storage;
    }

    public ChatMeta getChatMeta(UUID uuid) {
        User u = users.get(uuid);
        if (u == null) return ChatMeta.empty();

        String groupName = getPrimaryGroup(u);
        Group g = groupName == null ? null : groups.get(groupName.toLowerCase(Locale.ROOT));

        Meta gm = (g != null && g.getMeta() != null) ? g.getMeta() : new Meta();
        Meta um = (u.getMeta() != null) ? u.getMeta() : new Meta();

        String prefix = pick(um.getPrefix(), gm.getPrefix());
        String prefixColor = pick(um.getPrefixColor(), gm.getPrefixColor());

        String suffix = pick(um.getSuffix(), gm.getSuffix());
        String suffixColor = pick(um.getSuffixColor(), gm.getSuffixColor());

        String playerColor = pick(um.getPlayerColor(), gm.getPlayerColor());
        String chatColor = pick(um.getChatColor(), gm.getChatColor());

        // Formats are left blank for now
        return new ChatMeta(
                prefix, prefixColor, "",
                suffix, suffixColor, "",
                playerColor, "",
                chatColor, ""
        );
    }

    private static String pick(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return "";
    }




    // ----------------------------
    // Load / save
    // ----------------------------

    public void loadData() {
        groups.clear();
        groups.putAll(storage.loadGroups());

        if (groups.isEmpty()) {
            Group def = storage.loadDefaultGroup();
            groups.put(def.getName().toLowerCase(Locale.ROOT), def);
            storage.saveGroups(groups);
        }
    }

    public void saveData() {
        storage.saveGroups(groups);
        for (User u : users.values()) storage.saveUser(u);
    }

    // ----------------------------
    // User load
    // ----------------------------

    public void ensureUserLoaded(UUID uuid, String username) {
        if (uuid == null) return;
        users.computeIfAbsent(uuid, id -> {
            User u = storage.loadUser(id);
            if (u == null) u = new User(id);
            u.setUsername(username);

            if (getPrimaryGroup(u) == null) {
                u.getNodes().add(new Node("group.default", true));
            }

            storage.saveUser(u);
            return u;
        });
    }

    private User getOrLoadUser(UUID uuid) {
        if (uuid == null) return null;
        return users.computeIfAbsent(uuid, id -> {
            User u = storage.loadUser(id);
            if (u == null) u = new User(id);
            if (getPrimaryGroup(u) == null) {
                u.getNodes().add(new Node("group.default", true));
            }
            storage.saveUser(u);
            return u;
        });
    }

    public void unloadUser(UUID uuid) {
        if (uuid == null) return;
        User u = users.remove(uuid);
        if (u != null) storage.saveUser(u);
    }

    private Group getOrCreateGroup(String name) {
        if (name == null || name.isBlank()) return null;
        String key = name.toLowerCase(Locale.ROOT);
        Group g = groups.get(key);
        if (g != null) return g;

        g = new Group(key, 0);
        groups.put(key, g);
        storage.saveGroups(groups);
        return g;
    }

    // ----------------------------
    // Permission checks
    // ----------------------------

    public boolean hasPermission(UUID uuid, String permission) {
        if (uuid == null || permission == null || permission.isBlank()) return false;

        User u = getOrLoadUser(uuid);
        if (u == null) return false;

        long now = System.currentTimeMillis();

        for (Node n : u.getNodes()) {
            if (n == null || n.isExpired(now)) continue;
            if (matches(n.getKey(), permission)) return n.isValue();
        }

        String groupName = getPrimaryGroup(u);
        if (groupName == null) return false;

        Group g = groups.get(groupName.toLowerCase(Locale.ROOT));
        if (g == null) return false;

        for (Node n : g.getNodes()) {
            if (n == null || n.isExpired(now)) continue;
            if (matches(n.getKey(), permission)) return n.isValue();
        }

        return false;
    }


    private static boolean matches(String node, String perm) {
        if (node == null || node.isBlank()) return false;
        if (node.equals("*")) return true;
        if (node.equalsIgnoreCase(perm)) return true;
        if (node.endsWith(".*")) {
            String base = node.substring(0, node.length() - 2);
            return perm.startsWith(base);
        }
        return false;
    }

    // ----------------------------
    // Group resolution (EtherealPerms style)
    // ----------------------------

    public String getPrimaryGroup(UUID uuid) {
        User u = users.get(uuid);
        if (u == null) return null;
        return getPrimaryGroup(u);
    }

    private static String getPrimaryGroup(User u) {
        for (Node n : u.getNodes()) {
            if (n == null) continue;
            String k = n.getKey();
            if (k != null && k.startsWith("group.") && n.isValue()) {
                return k.substring("group.".length());
            }
        }
        return null;
    }

    // ----------------------------
    // Chat meta (prefix/colors)
    // ----------------------------

    // ----------------------------
    // Admin helpers for your commands
    // ----------------------------

    public boolean setUserGroup(UUID uuid, String group) {
        if (uuid == null || group == null || group.isBlank()) return false;
        User u = getOrLoadUser(uuid);
        if (u == null) return false;

        u.getNodes().removeIf(n -> n != null && n.getKey() != null && n.getKey().startsWith("group."));
        u.getNodes().add(new Node("group." + group.toLowerCase(Locale.ROOT), true));
        storage.saveUser(u);
        return true;
    }

    public boolean createGroup(String name, int weight) {
        if (name == null || name.isBlank()) return false;
        name = name.toLowerCase(Locale.ROOT);
        if (groups.containsKey(name)) return false;
        groups.put(name, new Group(name, weight));
        storage.saveGroups(groups);
        return true;
    }

    public Group getGroup(String name) {
        if (name == null) return null;
        return groups.get(name.toLowerCase(Locale.ROOT));
    }

    public Set<String> listGroups() {
        return new TreeSet<>(groups.keySet());
    }

    // =====================================================================
    // Hytale PermissionProvider adapter methods
    // These are what GoGPermissionProvider should call.
    // =====================================================================

    public void userAddPermissions(UUID uuid, Set<String> permissions) {
        if (uuid == null || permissions == null || permissions.isEmpty()) return;
        User u = getOrLoadUser(uuid);
        if (u == null) return;

        for (String p : permissions) {
            if (p == null || p.isBlank()) continue;
            u.getNodes().add(new Node(p.trim(), true));
        }

        storage.saveUser(u);
    }

    public void userRemovePermissions(UUID uuid, Set<String> permissions) {
        if (uuid == null || permissions == null || permissions.isEmpty()) return;
        User u = getOrLoadUser(uuid);
        if (u == null) return;

        Set<String> lower = new HashSet<>();
        for (String p : permissions) {
            if (p == null) continue;
            lower.add(p.toLowerCase(Locale.ROOT));
        }

        u.getNodes().removeIf(n -> {
            if (n == null) return false;
            String k = n.getKey();
            if (k == null) return false;
            return lower.contains(k.toLowerCase(Locale.ROOT));
        });

        storage.saveUser(u);
    }

    public Set<String> userListPermissions(UUID uuid) {
        User u = getOrLoadUser(uuid);
        if (u == null) return Collections.emptySet();

        long now = System.currentTimeMillis();
        Set<String> out = new HashSet<>();
        for (Node n : u.getNodes()) {
            if (n == null || n.isExpired(now)) continue;
            if (!n.isValue()) continue;
            String k = n.getKey();
            if (k == null || k.isBlank()) continue;
            out.add(k);
        }
        return out;
    }

    public void groupAddPermissions(String group, Set<String> permissions) {
        if (group == null || group.isBlank() || permissions == null || permissions.isEmpty()) return;
        Group g = getOrCreateGroup(group);
        if (g == null) return;

        for (String p : permissions) {
            if (p == null || p.isBlank()) continue;
            g.getNodes().add(new Node(p.trim(), true));
        }

        storage.saveGroups(groups);
    }

    public void groupRemovePermissions(String group, Set<String> permissions) {
        if (group == null || group.isBlank() || permissions == null || permissions.isEmpty()) return;
        Group g = groups.get(group.toLowerCase(Locale.ROOT));
        if (g == null) return;

        Set<String> lower = new HashSet<>();
        for (String p : permissions) {
            if (p == null) continue;
            lower.add(p.toLowerCase(Locale.ROOT));
        }

        g.getNodes().removeIf(n -> {
            if (n == null) return false;
            String k = n.getKey();
            if (k == null) return false;
            return lower.contains(k.toLowerCase(Locale.ROOT));
        });

        storage.saveGroups(groups);
    }

    public Set<String> groupListPermissions(String group) {
        if (group == null || group.isBlank()) return Collections.emptySet();
        Group g = groups.get(group.toLowerCase(Locale.ROOT));
        if (g == null) return Collections.emptySet();

        long now = System.currentTimeMillis();
        Set<String> out = new HashSet<>();
        for (Node n : g.getNodes()) {
            if (n == null || n.isExpired(now)) continue;
            if (!n.isValue()) continue;
            String k = n.getKey();
            if (k == null || k.isBlank()) continue;
            out.add(k);
        }
        return out;
    }

    public void userAddToGroup(UUID uuid, String group) {
        if (uuid == null || group == null || group.isBlank()) return;
        setUserGroup(uuid, group);
    }

    public void userRemoveFromGroup(UUID uuid, String group) {
        if (uuid == null || group == null || group.isBlank()) return;
        User u = getOrLoadUser(uuid);
        if (u == null) return;

        String target = "group." + group.toLowerCase(Locale.ROOT);
        u.getNodes().removeIf(n -> {
            if (n == null) return false;
            String k = n.getKey();
            if (k == null) return false;
            return k.equalsIgnoreCase(target);
        });

        if (getPrimaryGroup(u) == null) {
            u.getNodes().add(new Node("group.default", true));
        }

        storage.saveUser(u);
    }

    public Set<String> userListGroups(UUID uuid) {
        User u = getOrLoadUser(uuid);
        if (u == null) return Collections.emptySet();

        long now = System.currentTimeMillis();
        Set<String> out = new HashSet<>();
        for (Node n : u.getNodes()) {
            if (n == null || n.isExpired(now)) continue;
            if (!n.isValue()) continue;
            String k = n.getKey();
            if (k == null) continue;
            if (!k.startsWith("group.")) continue;
            out.add(k.substring("group.".length()));
        }
        return out;
    }
}
