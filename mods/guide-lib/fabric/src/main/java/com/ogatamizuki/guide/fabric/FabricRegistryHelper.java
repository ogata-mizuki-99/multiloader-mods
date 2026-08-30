package com.ogatamizuki.guide.fabric;

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
        unfreezeIntrusive(BuiltInRegistries.BLOCK);
        unfreezeIntrusive(BuiltInRegistries.ITEM);
        unfreezeIntrusive(BuiltInRegistries.BLOCK_ENTITY_TYPE);
        unfreezeIntrusive(BuiltInRegistries.ENTITY_TYPE);

        unfreeze(BuiltInRegistries.MENU);
        unfreeze(BuiltInRegistries.CREATIVE_MODE_TAB);
        unfreeze(BuiltInRegistries.DATA_COMPONENT_TYPE);
        unfreeze(BuiltInRegistries.SOUND_EVENT);
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
            try {
                Field allTagsField = MappedRegistry.class.getDeclaredField("allTags");
                allTagsField.setAccessible(true);
                Object currentTags = allTagsField.get(mapped);
                java.lang.reflect.Method isBoundMethod = currentTags.getClass().getMethod("isBound");
                isBoundMethod.setAccessible(true);
                if ((boolean) isBoundMethod.invoke(currentTags)) {
                    for (Class<?> inner : MappedRegistry.class.getDeclaredClasses()) {
                        if (inner.getSimpleName().equals("TagSet")) {
                            try {
                                java.lang.reflect.Method unboundMethod = inner.getDeclaredMethod("unbound");
                                unboundMethod.setAccessible(true);
                                Object unboundTags = unboundMethod.invoke(null);
                                allTagsField.set(mapped, unboundTags);
                            } catch (Exception ignored2) {
                            }
                            break;
                        }
                    }
                }
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
