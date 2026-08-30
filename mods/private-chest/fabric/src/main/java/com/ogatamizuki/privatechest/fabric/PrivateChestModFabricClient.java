package com.ogatamizuki.privatechest.fabric;

import com.ogatamizuki.privatechest.LockerMenu;
import com.ogatamizuki.privatechest.LockerScreen;
import com.ogatamizuki.privatechest.client.LockerAwareSkullRenderer;
import com.ogatamizuki.privatechest.client.LockerBlockEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class PrivateChestModFabricClient implements ClientModInitializer {

    @FunctionalInterface
    public interface ScreenFactory<M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> {
        U create(M menu, Inventory inventory, net.minecraft.network.chat.Component title);
    }

    @Override
    public void onInitializeClient() {
        PrivateChestModFabric.LOGGER.info("Private Locker Chest Mod (Fabric Client) Initializing...");

        registerScreen(
                PrivateChestModFabric.LOCKER_MENU_TYPE,
                (LockerMenu menu, Inventory inv, net.minecraft.network.chat.Component title) -> new LockerScreen(menu, inv, title)
        );

        BlockEntityRendererRegistry.register(BlockEntityType.SKULL, LockerAwareSkullRenderer::new);
        BlockEntityRendererRegistry.register(PrivateChestModFabric.LOCKER_BLOCK_ENTITY_TYPE, LockerBlockEntityRenderer::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void registerScreen(
            MenuType<M> type, ScreenFactory<M, U> factory) {
        try {
            Class<?> screenCtorInterface = null;
            for (Class<?> inner : MenuScreens.class.getDeclaredClasses()) {
                if (inner.isInterface() && inner.getSimpleName().equals("ScreenConstructor")) {
                    screenCtorInterface = inner;
                    break;
                }
            }
            if (screenCtorInterface == null) {
                throw new IllegalStateException("MenuScreens.ScreenConstructor interface not found");
            }

            final Class<?> ctorIface = screenCtorInterface;
            Object proxy = Proxy.newProxyInstance(
                    ctorIface.getClassLoader(),
                    new Class<?>[]{ctorIface},
                    (p, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return method.invoke(p, args);
                        }
                        if (method.isDefault()) {
                            return java.lang.reflect.InvocationHandler.invokeDefault(p, method, args);
                        }
                        return factory.create((M) args[0], (Inventory) args[1],
                                (net.minecraft.network.chat.Component) args[2]);
                    }
            );

            Method registerMethod = MenuScreens.class.getDeclaredMethod("register", MenuType.class, screenCtorInterface);
            registerMethod.setAccessible(true);
            registerMethod.invoke(null, type, proxy);
        } catch (Exception e) {
            PrivateChestModFabric.LOGGER.error("Failed to register menu screen for {}", type, e);
        }
    }
}
