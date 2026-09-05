package com.ogatamizuki.nickname;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class NicknameStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, String> nicknames = new ConcurrentHashMap<>();

    public static Map<UUID, String> getNicknames() {
        return nicknames;
    }

    public static String getNickname(UUID uuid) {
        return nicknames.get(uuid);
    }

    public static void setNickname(UUID uuid, String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            nicknames.remove(uuid);
        } else {
            nicknames.put(uuid, nickname);
        }
    }

    public static void clear() {
        nicknames.clear();
    }

    public static void putAll(Map<UUID, String> map) {
        nicknames.putAll(map);
    }

    public static void load(MinecraftServer server) {
        nicknames.clear();
        File file = getStorageFile(server);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> rawMap = GSON.fromJson(reader, type);
                if (rawMap != null) {
                    for (Map.Entry<String, String> entry : rawMap.entrySet()) {
                        try {
                            nicknames.put(UUID.fromString(entry.getKey()), entry.getValue());
                        } catch (IllegalArgumentException e) {
                            NicknameCommon.LOGGER.error("Failed to parse UUID key from JSON: {}", entry.getKey());
                        }
                    }
                }
            } catch (IOException e) {
                NicknameCommon.LOGGER.error("Failed to load nicknames JSON file", e);
            }
        }
    }

    public static void save(MinecraftServer server) {
        File file = getStorageFile(server);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        Map<String, String> rawMap = new HashMap<>();
        for (Map.Entry<UUID, String> entry : nicknames.entrySet()) {
            rawMap.put(entry.getKey().toString(), entry.getValue());
        }

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(rawMap, writer);
        } catch (IOException e) {
            NicknameCommon.LOGGER.error("Failed to save nicknames JSON file", e);
        }
    }

    public static CompletableFuture<Void> saveAsync(MinecraftServer server) {
        File file = getStorageFile(server);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        Map<String, String> rawMap = new HashMap<>();
        for (Map.Entry<UUID, String> entry : nicknames.entrySet()) {
            rawMap.put(entry.getKey().toString(), entry.getValue());
        }

        return CompletableFuture.runAsync(() -> {
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(rawMap, writer);
            } catch (IOException e) {
                NicknameCommon.LOGGER.error("Failed to save nicknames JSON file asynchronously", e);
            }
        });
    }

    private static File getStorageFile(MinecraftServer server) {
        // world/data/nicknames.json に保存
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("nicknames.json").toFile();
    }
}
