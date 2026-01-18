package com.goldorgrave.essentials.perms.models;

import java.util.Map;

public final class Node {
    private String key;
    private boolean value;
    private Long expiry;
    private Map<String, String> context;

    public Node() {}

    public Node(String key, boolean value) {
        this.key = key;
        this.value = value;
    }

    public Node(String key, boolean value, Long expiry, Map<String, String> context) {
        this.key = key;
        this.value = value;
        this.expiry = expiry;
        this.context = context;
    }

    public String getKey() { return key; }
    public boolean isValue() { return value; }
    public Long getExpiry() { return expiry; }
    public Map<String, String> getContext() { return context; }

    public void setKey(String key) { this.key = key; }
    public void setValue(boolean value) { this.value = value; }
    public void setExpiry(Long expiry) { this.expiry = expiry; }
    public void setContext(Map<String, String> context) { this.context = context; }

    public boolean isExpired(long nowMs) {
        return expiry != null && expiry > 0 && nowMs >= expiry;
    }
}
