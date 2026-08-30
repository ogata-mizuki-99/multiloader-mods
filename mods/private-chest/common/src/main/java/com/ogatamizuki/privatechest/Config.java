package com.ogatamizuki.privatechest;

public final class Config {
    private static volatile boolean enableLockerCrafting = true;

    private Config() {}

    public static boolean isEnableLockerCrafting() {
        return enableLockerCrafting;
    }

    public static void setEnableLockerCrafting(boolean value) {
        enableLockerCrafting = value;
    }
}
