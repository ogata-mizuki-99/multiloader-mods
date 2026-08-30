package com.ogatamizuki.deconstructor.fabric;

import com.ogatamizuki.deconstructor.DeconstructorBlocks;
import com.ogatamizuki.deconstructor.DeconstructorCommon;
import com.ogatamizuki.deconstructor.DeconstructorScreen;
import com.ogatamizuki.deconstructor.EnchantmentManagerRenderer;
import com.ogatamizuki.deconstructor.EnchantmentManagerScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DeconstructorModFabricClient implements ClientModInitializer {

    @FunctionalInterface
    public interface ScreenFactory<M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> {
        U create(M menu, Inventory inventory, net.minecraft.network.chat.Component title);
    }

    @Override
    public void onInitializeClient() {
        registerScreen(
                DeconstructorBlocks.DECONSTRUCTOR_MENU_TYPE.get(),
                (com.ogatamizuki.deconstructor.DeconstructorMenu menu,
                 Inventory inv,
                 net.minecraft.network.chat.Component title) -> new DeconstructorScreen(menu, inv, title)
        );
        registerScreen(
                DeconstructorBlocks.ENCHANT_MANAGER_MENU_TYPE.get(),
                (com.ogatamizuki.deconstructor.EnchantmentManagerMenu menu,
                 Inventory inv,
                 net.minecraft.network.chat.Component title) -> new EnchantmentManagerScreen(menu, inv, title)
        );

        BlockEntityRendererRegistry.register(
                DeconstructorBlocks.ENCHANT_MANAGER_BLOCK_ENTITY_TYPE.get(),
                EnchantmentManagerRenderer::new
        );
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
            DeconstructorCommon.LOGGER.error("Failed to register menu screen for {}", type, e);
        }
    }
}
