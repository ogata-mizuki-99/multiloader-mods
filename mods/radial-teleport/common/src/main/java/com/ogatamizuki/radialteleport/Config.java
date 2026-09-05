package com.ogatamizuki.radialteleport;

public final class Config {
    private static volatile boolean enableCraftingRecipe = true;
    private static volatile boolean enableWaypoints = true;
    private static volatile int maxWaypointsPerPlayer = 8;
    private static volatile int teleportCooldownTicks = 0;

    private Config() {
    }

    public static boolean isEnableCraftingRecipe() {
        return enableCraftingRecipe;
    }

    public static void setEnableCraftingRecipe(boolean value) {
        enableCraftingRecipe = value;
    }

    public static boolean isEnableWaypoints() {
        return enableWaypoints;
    }

    public static void setEnableWaypoints(boolean value) {
        enableWaypoints = value;
    }

    public static int getMaxWaypointsPerPlayer() {
        return maxWaypointsPerPlayer;
    }

    public static void setMaxWaypointsPerPlayer(int value) {
        maxWaypointsPerPlayer = Math.max(1, Math.min(32, value));
    }

    public static int getTeleportCooldownTicks() {
        return teleportCooldownTicks;
    }

    public static void setTeleportCooldownTicks(int value) {
        teleportCooldownTicks = Math.max(0, Math.min(72000, value));
    }
}
