// File: com/goldorgrave/essentials/model/Rank.java
package com.goldorgrave.essentials.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class Rank {
    private final String name;
    private final int priority;
    private final LinkedHashSet<String> permissions;

    public Rank(String name, int priority, Set<String> permissions) {
        this.name = normalizeName(name);
        this.priority = priority;
        this.permissions = new LinkedHashSet<>();
        if (permissions != null) {
            for (String p : permissions) {
                String n = normalizePerm(p);
                if (n != null) this.permissions.add(n);
            }
        }
    }

    public String name() {
        return name;
    }

    public int priority() {
        return priority;
    }

    public Set<String> permissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public Rank withPermissionAdded(String perm) {
        LinkedHashSet<String> next = new LinkedHashSet<>(permissions);
        String n = normalizePerm(perm);
        if (n != null) next.add(n);
        return new Rank(name, priority, next);
    }

    public Rank withPermissionRemoved(String perm) {
        LinkedHashSet<String> next = new LinkedHashSet<>(permissions);
        String n = normalizePerm(perm);
        if (n != null) next.remove(n);
        return new Rank(name, priority, next);
    }

    public static String normalizeName(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isEmpty()) return null;
        return v.toLowerCase(Locale.ROOT);
    }

    public static String normalizePerm(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isEmpty()) return null;
        return v.toLowerCase(Locale.ROOT);
    }
}
