package com.ogatamizuki.elytraslot;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Config {
    public static class ConfigValue<T> {
        private T value;
        private Supplier<T> getter;
        private Consumer<T> setter;
        private Runnable saver;

        public ConfigValue(T defaultValue) {
            this.value = defaultValue;
        }

        public T get() {
            return getter != null ? getter.get() : value;
        }

        public void set(T val) {
            this.value = val;
            if (setter != null) {
                setter.accept(val);
            }
        }

        public void save() {
            if (saver != null) {
                saver.run();
            }
        }

        public void bind(Supplier<T> getter, Consumer<T> setter, Runnable saver) {
            this.getter = getter;
            this.setter = setter;
            this.saver = saver;
        }
    }

    // Default Values
    public static final ConfigValue<Integer> ELYTRA_SLOT_X = new ConfigValue<>(77);
    public static final ConfigValue<Integer> ELYTRA_SLOT_Y = new ConfigValue<>(26);
    public static final ConfigValue<Integer> FIREWORK_SLOT_X = new ConfigValue<>(77);
    public static final ConfigValue<Integer> FIREWORK_SLOT_Y = new ConfigValue<>(8);
    public static final ConfigValue<Integer> CREATIVE_ELYTRA_SLOT_X = new ConfigValue<>(126);
    public static final ConfigValue<Integer> CREATIVE_ELYTRA_SLOT_Y = new ConfigValue<>(33);
    public static final ConfigValue<Integer> CREATIVE_FIREWORK_SLOT_X = new ConfigValue<>(126);
    public static final ConfigValue<Integer> CREATIVE_FIREWORK_SLOT_Y = new ConfigValue<>(6);

    public static final ConfigValue<Boolean> HUD_ENABLED = new ConfigValue<>(true);
    public static final ConfigValue<Integer> ELYTRA_HUD_X = new ConfigValue<>(-150);
    public static final ConfigValue<Integer> ELYTRA_HUD_Y = new ConfigValue<>(-22);
    public static final ConfigValue<Integer> FIREWORK_HUD_X = new ConfigValue<>(-170);
    public static final ConfigValue<Integer> FIREWORK_HUD_Y = new ConfigValue<>(-22);
    public static final ConfigValue<Double> WARNING_THRESHOLD = new ConfigValue<>(0.05);
}
