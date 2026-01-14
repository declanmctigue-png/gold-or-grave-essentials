package com.goldorgrave.essentials.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonStore {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonStore() {}

    public static <T> T readOrCreate(Path path, Type type, T defaultValue) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, GSON.toJson(defaultValue));
                return defaultValue;
            }
            String json = Files.readString(path);
            T obj = GSON.fromJson(json, type);
            return obj == null ? defaultValue : obj;
        } catch (IOException e) {
            return defaultValue;
        }
    }

    public static void write(Path path, Object obj) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(obj));
        } catch (IOException ignored) {}
    }

    public static <T> Type typeOf(Class<T> clazz) {
        return TypeToken.get(clazz).getType();
    }
}
