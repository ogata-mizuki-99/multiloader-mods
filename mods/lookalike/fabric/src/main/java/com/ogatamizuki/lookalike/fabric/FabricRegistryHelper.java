package com.ogatamizuki.lookalike.fabric;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;

public final class FabricRegistryHelper {
    private FabricRegistryHelper() {}

    public static void prepare() {
        unfreezeIntrusive(BuiltInRegistries.ITEM);
        unfreeze(BuiltInRegistries.CREATIVE_MODE_TAB);
    }

    @SuppressWarnings("unchecked")
    public static <T, V extends T> V register(Registry<T> registry, Identifier id, V value) {
        unfreeze(registry);
        return (V) Registry.register((Registry<Object>) registry, id, value);
    }

    @SuppressWarnings("unchecked")
    public static <T, V extends T> V register(Registry<T> registry, ResourceKey<?> key, V value) {
        unfreeze(registry);
        return (V) Registry.register((Registry<Object>) registry, (ResourceKey<Object>) key, value);
    }

    public static void unfreeze(Registry<?> registry) {
        if (registry instanceof MappedRegistry<?> mapped) {
            try {
                Field frozenField = MappedRegistry.class.getDeclaredField("frozen");
                frozenField.setAccessible(true);
                frozenField.set(mapped, false);
            } catch (Exception ignored) {
            }
        }
    }

    public static void unfreezeIntrusive(Registry<?> registry) {
        unfreeze(registry);
        if (registry instanceof MappedRegistry<?> mapped) {
            try {
                Field intrusiveField = MappedRegistry.class.getDeclaredField("unregisteredIntrusiveHolders");
                intrusiveField.setAccessible(true);
                if (intrusiveField.get(mapped) == null) {
                    intrusiveField.set(mapped, new IdentityHashMap<>());
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static CreativeModeTab.Builder createTabBuilder() {
        return FabricCreativeModeTab.builder();
    }
}
