package com.ogatamizuki.economy.backend;

/** {@link EconomyBackend} のファクトリ（単体版は LOCAL 固定）。 */
public final class EconomyBackends {
    private static EconomyBackend instance;

    private EconomyBackends() {
    }

    public static EconomyBackend get() {
        if (instance == null) {
            instance = new EconomyLocalBackend();
        }
        return instance;
    }

    public static void reload() {
        instance = null;
    }
}
