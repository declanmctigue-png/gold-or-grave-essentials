package com.goldorgrave.essentials.perms.data;

import com.goldorgrave.essentials.perms.models.Group;
import com.goldorgrave.essentials.perms.models.Node;
import com.goldorgrave.essentials.perms.models.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class PermsStorage {

    private final Path root;
    private final Path usersDir;
    private final Path groupsFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public PermsStorage(Path pluginDataDir) {
        // You can choose exactly where, but keep it stable
        this.root = pluginDataDir.resolve("perms");
        this.usersDir = root.resolve("users");
        this.groupsFile = root.resolve("groups.json");

        try {
            Files.createDirectories(root);
            Files.createDirectories(usersDir);
        } catch (Throwable ignored) {}
    }

    public Map<String, Group> loadGroups() {
        try {
            if (!Files.exists(groupsFile)) return new HashMap<>();
            String json = Files.readString(groupsFile);
            java.lang.reflect.Type t = new TypeToken<Map<String, Group>>(){}.getType();
            Map<String, Group> m = gson.fromJson(json, t);
            return m == null ? new HashMap<>() : m;
        } catch (Throwable t) {
            return new HashMap<>();
        }
    }

    public void saveGroups(Map<String, Group> groups) {
        try {
            Files.createDirectories(root);
            Files.writeString(groupsFile, gson.toJson(groups));
        } catch (Throwable ignored) {}
    }

    public User loadUser(UUID uuid) {
        try {
            Path p = usersDir.resolve(uuid + ".json");
            if (!Files.exists(p)) return null;
            String json = Files.readString(p);
            return gson.fromJson(json, User.class);
        } catch (Throwable t) {
            return null;
        }
    }

    public void saveUser(User user) {
        try {
            if (user == null || user.getUuid() == null) return;
            Files.createDirectories(usersDir);
            Path p = usersDir.resolve(user.getUuid() + ".json");
            Files.writeString(p, gson.toJson(user));
        } catch (Throwable ignored) {}
    }

    public Group loadDefaultGroup() {
        Group g = new Group("default", 0);
        g.getNodes().add(new Node("gog.default", true));
        return g;
    }
}
