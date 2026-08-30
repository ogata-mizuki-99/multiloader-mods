package com.ogatamizuki.lookalike;

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
            if (setter != null) setter.accept(val);
        }

        public void save() {
            if (saver != null) saver.run();
        }

        public void bind(Supplier<T> getter, Consumer<T> setter, Runnable saver) {
            this.getter = getter;
            this.setter = setter;
            this.saver = saver;
        }
    }

    public static final ConfigValue<Integer> disguiseDurationSeconds = new ConfigValue<>(60);
    public static final ConfigValue<Boolean> allowDefaultPlayerList = new ConfigValue<>(false);
    public static final ConfigValue<Boolean> hideAllNametags = new ConfigValue<>(false);
    public static final ConfigValue<Boolean> enableMirrorCrafting = new ConfigValue<>(true);
    public static final ConfigValue<Integer> defaultCastTimeSeconds = new ConfigValue<>(0);
    public static final ConfigValue<String> defaultEffectTemplate = new ConfigValue<>("WITCH_SMOKE");
}
