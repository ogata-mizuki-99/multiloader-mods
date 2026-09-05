package com.ogatamizuki.economy.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ogatamizuki.economy.EconomyCommon;
import com.ogatamizuki.economy.EconomyRuntimeConfig;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EconomyFabricConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "economy.json";

    public boolean enableBalanceHud = true;
    public boolean enableActionRewards = true;
    public boolean enableEtfUpdates = true;
    public int rewardChatAggregateSeconds = 2;

    public static void load() {
        Path path = configPath();
        EconomyFabricConfig data = new EconomyFabricConfig();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                data = GSON.fromJson(reader, EconomyFabricConfig.class);
                if (data == null) {
                    data = new EconomyFabricConfig();
                }
            } catch (IOException e) {
                EconomyCommon.LOGGER.warn("Failed to load economy config, using defaults: {}", e.getMessage());
                data = new EconomyFabricConfig();
            }
        }
        data.applyToRuntime();
        data.save();
    }

    public void save() {
        try {
            Files.createDirectories(configPath().getParent());
            try (Writer writer = Files.newBufferedWriter(configPath())) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            EconomyCommon.LOGGER.warn("Failed to save economy config: {}", e.getMessage());
        }
    }

    public void applyToRuntime() {
        EconomyRuntimeConfig.enableBalanceHud = enableBalanceHud;
        EconomyRuntimeConfig.enableActionRewards = enableActionRewards;
        EconomyRuntimeConfig.enableEtfUpdates = enableEtfUpdates;
        EconomyRuntimeConfig.rewardChatAggregateSeconds = Math.max(0, Math.min(30, rewardChatAggregateSeconds));
    }

    public void syncFromRuntime() {
        enableBalanceHud = EconomyRuntimeConfig.enableBalanceHud;
        enableActionRewards = EconomyRuntimeConfig.enableActionRewards;
        enableEtfUpdates = EconomyRuntimeConfig.enableEtfUpdates;
        rewardChatAggregateSeconds = EconomyRuntimeConfig.rewardChatAggregateSeconds;
    }

    private static Path configPath() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public EconomyFabricConfig() {}
}
