package com.ogatamizuki.deconstructor;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public final class DeconstructorBlocks {
    public static Supplier<DeconstructorBlock> DECONSTRUCTOR = () -> null;
    public static Supplier<DeconstructorBlock> PRECISION_DECONSTRUCTOR = () -> null;
    public static Supplier<EnchantmentManagerBlock> ENCHANT_MANAGER = () -> null;

    public static Supplier<BlockItem> DECONSTRUCTOR_ITEM = () -> null;
    public static Supplier<BlockItem> PRECISION_DECONSTRUCTOR_ITEM = () -> null;
    public static Supplier<BlockItem> ENCHANT_MANAGER_ITEM = () -> null;

    public static Supplier<BlockEntityType<DeconstructorBlockEntity>> DECONSTRUCTOR_BLOCK_ENTITY_TYPE = () -> null;
    public static Supplier<BlockEntityType<EnchantmentManagerBlockEntity>> ENCHANT_MANAGER_BLOCK_ENTITY_TYPE = () -> null;

    public static Supplier<MenuType<DeconstructorMenu>> DECONSTRUCTOR_MENU_TYPE = () -> null;
    public static Supplier<MenuType<EnchantmentManagerMenu>> ENCHANT_MANAGER_MENU_TYPE = () -> null;

    private DeconstructorBlocks() {}
}
