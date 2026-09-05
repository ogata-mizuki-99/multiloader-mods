package com.ogatamizuki.privatechest.fabric;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.flag.FeatureFlagSet;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;
import java.util.Set;

public final class FabricRegistryHelper {
    private FabricRegistryHelper() {}

    @FunctionalInterface
    public interface BlockEntityTypeSupplier<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }

    @FunctionalInterface
    public interface MenuFactory<T extends AbstractContainerMenu> {
        T create(int containerId, Inventory inventory);
    }

    public static void prepare() {
        unfreezeIntrusive(BuiltInRegistries.BLOCK);
        unfreezeIntrusive(BuiltInRegistries.ITEM);
        unfreezeIntrusive(BuiltInRegistries.BLOCK_ENTITY_TYPE);
        unfreeze(BuiltInRegistries.MENU);
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

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(
            BlockEntityTypeSupplier<T> factory,
            Block... validBlocks
    ) {
        try {
            unfreezeIntrusive(BuiltInRegistries.BLOCK_ENTITY_TYPE);
            for (Constructor<?> c : BlockEntityType.class.getDeclaredConstructors()) {
                if (c.getParameterCount() == 2) {
                    c.setAccessible(true);
                    Class<?> supplierInterface = c.getParameterTypes()[0];
                    Object supplierProxy = Proxy.newProxyInstance(
                            supplierInterface.getClassLoader(),
                            new Class<?>[]{supplierInterface},
                            (proxy, method, args) -> factory.create((BlockPos) args[0], (BlockState) args[1])
                    );
                    return (BlockEntityType<T>) c.newInstance(supplierProxy, Set.of(validBlocks));
                }
            }
            throw new IllegalStateException("No 2-arg BlockEntityType constructor found");
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate BlockEntityType", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractContainerMenu> MenuType<T> createMenuType(MenuFactory<T> factory, FeatureFlagSet flags) {
        try {
            for (Constructor<?> c : MenuType.class.getDeclaredConstructors()) {
                if (c.getParameterCount() == 2) {
                    c.setAccessible(true);
                    Class<?> supplierInterface = c.getParameterTypes()[0];
                    Object supplierProxy = Proxy.newProxyInstance(
                            supplierInterface.getClassLoader(),
                            new Class<?>[]{supplierInterface},
                            (proxy, method, args) -> factory.create((int) args[0], (Inventory) args[1])
                    );
                    return (MenuType<T>) c.newInstance(supplierProxy, flags);
                }
            }
            throw new IllegalStateException("No 2-arg MenuType constructor found");
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate MenuType", e);
        }
    }
}
