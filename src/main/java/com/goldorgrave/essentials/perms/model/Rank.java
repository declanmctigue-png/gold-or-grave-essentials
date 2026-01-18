// File: com/goldorgrave/essentials/model/Rank.java
package com.goldorgrave.essentials.perms.model;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public record Rank(String name, int priority, Set<String> permissions, String prefix) {

    // Backwards compatible constructor: existing code with 3 args still works
    public Rank(String name, int priority, Set<String> permissions) {
        this(name, priority, permissions, null);
    }

    public static String normalizeName(String name) {
        if (name == null) return null;
        String n = name.trim().toLowerCase(Locale.ROOT);
        if (n.isBlank()) return null;
        return n;
    }

    public static String normalizePerm(String perm) {
        if (perm == null) return null;
        String p = perm.trim().toLowerCase(Locale.ROOT);
        if (p.isBlank()) return null;
        return p;
    }

    public Rank withPermissionAdded(String perm) {
        String p = normalizePerm(perm);
        if (p == null) return this;

        Set<String> next = new HashSet<>(permissions == null ? Set.of() : permissions);
        next.add(p);
        return new Rank(name, priority, next, prefix);
    }

    public Rank withPermissionRemoved(String perm) {
        String p = normalizePerm(perm);
        if (p == null) return this;

        Set<String> next = new HashSet<>(permissions == null ? Set.of() : permissions);
        next.remove(p);
        return new Rank(name, priority, next, prefix);
    }

    public Rank withPrefix(String prefixOrNull) {
        String v = prefixOrNull;
        if (v != null && v.isBlank()) v = null;
        return new Rank(name, priority, permissions, v);
    }
}
