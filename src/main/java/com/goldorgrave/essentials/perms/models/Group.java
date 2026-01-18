package com.goldorgrave.essentials.perms.models;

import java.util.HashSet;
import java.util.Set;

public final class Group {
    private String name;
    private int weight;
    private Set<Node> nodes = new HashSet<>();
    private Meta meta = new Meta();

    public Group() {}

    public Group(String name, int weight) {
        this.name = name;
        this.weight = weight;
    }

    public String getName() { return name; }
    public int getWeight() { return weight; }
    public Set<Node> getNodes() { return nodes; }
    public Meta getMeta() { return meta; }

    public void setName(String name) { this.name = name; }
    public void setWeight(int weight) { this.weight = weight; }
    public void setNodes(Set<Node> nodes) { this.nodes = nodes; }
    public void setMeta(Meta meta) { this.meta = meta; }
}
