package com.ogatamizuki.economy;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class EconomyRegistries {
    public static EntityType<EconomyNpc> ECONOMY_NPC;
    public static EntityType<LoanNpc> LOAN_NPC;

    public static Block ATM_BLOCK;
    public static Item ATM_BLOCK_ITEM;

    public static Block ECONOMY_ADMIN_BLOCK;
    public static Item ECONOMY_ADMIN_BLOCK_ITEM;

    public static Item GOLD_COIN;
    public static Item SILVER_COIN;
    public static Item BRONZE_COIN;

    public static Item MOBILE_TERMINAL;
    public static Item RANKING_COMPILER;
    public static Item RANKING_VIEWER;

    public static Item ECONOMY_NPC_SPAWN_EGG;
    public static Item LOAN_NPC_SPAWN_EGG;

    public static CreativeModeTab EXAMPLE_TAB;

    private EconomyRegistries() {}
}
