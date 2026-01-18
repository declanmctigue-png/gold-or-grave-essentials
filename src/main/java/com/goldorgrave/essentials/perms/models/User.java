package com.goldorgrave.essentials.perms.models;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class User {
    private UUID uuid;
    private String username;
    private Set<Node> nodes = new HashSet<>();
    private Meta meta = new Meta();

    public User() {}

    public User(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public Set<Node> getNodes() { return nodes; }
    public Meta getMeta() { return meta; }

    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public void setUsername(String username) { this.username = username; }
    public void setNodes(Set<Node> nodes) { this.nodes = nodes; }
    public void setMeta(Meta meta) { this.meta = meta; }
}
