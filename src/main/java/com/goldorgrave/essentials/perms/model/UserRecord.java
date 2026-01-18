package com.goldorgrave.essentials.perms.model;

import java.util.*;

public final class UserRecord {

    private final UUID uuid;
    private final Set<String> ranks = new HashSet<>();
    private final Map<String, Boolean> permissions = new HashMap<>();

    public UserRecord(UUID uuid) {
        this.uuid = uuid;
        this.ranks.add("default");
    }

    public UUID uuid() {
        return uuid;
    }

    public Set<String> ranks() {
        return ranks;
    }

    public Map<String, Boolean> permissions() {
        return permissions;
    }
}