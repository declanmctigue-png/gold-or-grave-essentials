// File: com/goldorgrave/essentials/storage/RankService.java
package com.goldorgrave.essentials.storage;

import com.goldorgrave.essentials.perms.model.Rank;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class RankService {

    private final Map<String, Rank> ranksByName = new ConcurrentHashMap<>();
    private final Map<UUID, String> userRankByUuid = new ConcurrentHashMap<>();

    public void ensureDefaults() {
        if (!ranksByName.containsKey("default")) {
            new Rank("default", 0, Set.of(), null);
        }
        if (!ranksByName.containsKey("admin")) {
            new Rank("admin", 100, Set.of("gog.*"), "&c[ADMIN]&r ");
        }
    }

    public Set<String> listRanks() {
        return new TreeSet<>(ranksByName.keySet());
    }

    public Rank getRank(String name) {
        String n = Rank.normalizeName(name);
        if (n == null) return null;
        return ranksByName.get(n);
    }

    public boolean createRank(String name, int priority) {
        String n = Rank.normalizeName(name);
        if (n == null) return false;
        if (ranksByName.containsKey(n)) return false;
        ranksByName.put(n, new Rank(n, priority, Set.of(), null));
        return true;
    }

    public void upsertRank(Rank rank) {
        if (rank == null || rank.name() == null) return;
        ranksByName.put(rank.name(), rank);
    }

    public boolean deleteRank(String name) {
        String n = Rank.normalizeName(name);
        if (n == null) return false;
        if ("default".equals(n)) return false;

        Rank removed = ranksByName.remove(n);
        if (removed == null) return false;

        for (Map.Entry<UUID, String> e : userRankByUuid.entrySet()) {
            if (n.equals(e.getValue())) e.setValue("default");
        }
        return true;
    }

    public String getUserRank(UUID uuid) {
        if (uuid == null) return "default";
        return userRankByUuid.getOrDefault(uuid, "default");
    }

    public boolean setUserRank(UUID uuid, String rankName) {
        if (uuid == null) return false;
        String n = Rank.normalizeName(rankName);
        if (n == null) return false;
        if (!ranksByName.containsKey(n)) return false;
        userRankByUuid.put(uuid, n);
        return true;
    }

    public boolean addPermToRank(String rankName, String perm) {
        Rank r = getRank(rankName);
        if (r == null) return false;
        upsertRank(r.withPermissionAdded(perm));
        return true;
    }

    public boolean removePermFromRank(String rankName, String perm) {
        Rank r = getRank(rankName);
        if (r == null) return false;
        upsertRank(r.withPermissionRemoved(perm));
        return true;
    }

    // ----------------------------
    // Prefix support
    // ----------------------------

    /**
     * Returns the configured prefix for a rank, or null if none exists.
     */
    public String getRankPrefix(String rankName) {
        Rank r = getRank(rankName);
        if (r == null) return null;
        return r.prefix();
    }

    /**
     * Sets (or clears) the configured prefix for a rank.
     * Pass null or blank to clear.
     */
    public boolean setRankPrefix(String rankName, String prefixOrNull) {
        Rank r = getRank(rankName);
        if (r == null) return false;

        Rank next = r.withPrefix(prefixOrNull);
        upsertRank(next);
        return true;
    }

    public boolean hasPermission(UUID uuid, String perm) {
        String p = Rank.normalizePerm(perm);
        if (p == null) return true;

        String rankName = getUserRank(uuid);
        Rank r = getRank(rankName);
        if (r == null) r = getRank("default");
        if (r == null) return false;

        return matchesAny(r.permissions(), p);
    }

    private static boolean matchesAny(Set<String> nodes, String perm) {
        if (nodes == null || nodes.isEmpty()) return false;

        if (nodes.contains("*")) return true;
        if (nodes.contains(perm)) return true;

        for (String node : nodes) {
            if (node == null) continue;
            if (node.endsWith(".*")) {
                String prefix = node.substring(0, node.length() - 2);
                if (perm.equals(prefix)) return true;
                if (perm.startsWith(prefix + ".")) return true;
            }
        }
        return false;
    }
}
