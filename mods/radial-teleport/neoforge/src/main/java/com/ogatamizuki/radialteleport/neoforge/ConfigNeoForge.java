package com.ogatamizuki.radialteleport.neoforge;

import com.ogatamizuki.radialteleport.Config;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ConfigNeoForge {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_CRAFTING_RECIPE = BUILDER
            .comment("If true, the Teleport Compass can be crafted with the configured recipe.",
                    "If false, obtain it via /radialteleport give or creative mode.")
            .define("enableCraftingRecipe", true);

    public static final ModConfigSpec.BooleanValue ENABLE_WAYPOINTS = BUILDER
            .comment("If true, players can save personal waypoints and teleport to them from the radial menu.")
            .define("enableWaypoints", true);

    public static final ModConfigSpec.IntValue MAX_WAYPOINTS_PER_PLAYER = BUILDER
            .comment("Maximum number of personal waypoints each player may save.")
            .defineInRange("maxWaypointsPerPlayer", 8, 1, 32);

    public static final ModConfigSpec.IntValue TELEPORT_COOLDOWN_TICKS = BUILDER
            .comment("Cooldown between teleports in ticks (20 = 1 second). 0 disables cooldown.")
            .defineInRange("teleportCooldownTicks", 0, 0, 72000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ConfigNeoForge() {}

    public static void sync() {
        Config.setEnableCraftingRecipe(ENABLE_CRAFTING_RECIPE.get());
        Config.setEnableWaypoints(ENABLE_WAYPOINTS.get());
        Config.setMaxWaypointsPerPlayer(MAX_WAYPOINTS_PER_PLAYER.get());
        Config.setTeleportCooldownTicks(TELEPORT_COOLDOWN_TICKS.get());
    }

    public static void updateFromPush(boolean enableCrafting, boolean enableWaypoints, int maxWaypoints, int cooldownTicks) {
        ENABLE_CRAFTING_RECIPE.set(enableCrafting);
        ENABLE_CRAFTING_RECIPE.save();
        ENABLE_WAYPOINTS.set(enableWaypoints);
        ENABLE_WAYPOINTS.save();
        MAX_WAYPOINTS_PER_PLAYER.set(maxWaypoints);
        MAX_WAYPOINTS_PER_PLAYER.save();
        TELEPORT_COOLDOWN_TICKS.set(cooldownTicks);
        TELEPORT_COOLDOWN_TICKS.save();
        sync();
    }
}
