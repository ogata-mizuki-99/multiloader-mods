package com.ogatamizuki.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyItemMatcherTest {

    @Test
    void matchIdentityKey_includesPotion() {
        String key = EconomyItemMatcher.matchIdentityKey(
                "minecraft:potion", "minecraft:strength", null, null);
        assertEquals("minecraft:potion|potion=minecraft:strength", key);
    }

    @Test
    void matchIdentityKey_includesEnchantmentAndLevel() {
        String key = EconomyItemMatcher.matchIdentityKey(
                "minecraft:enchanted_book", null, "minecraft:efficiency", 5);
        assertEquals("minecraft:enchanted_book|ench=minecraft:efficiency|lvl=5", key);
    }

    @Test
    void matchIdentityKey_plainItem() {
        assertEquals("minecraft:iron_ingot",
                EconomyItemMatcher.matchIdentityKey("minecraft:iron_ingot", null, null, null));
    }

    @Test
    void hasVariantMatch_detectsFields() {
        var plain = new com.ogatamizuki.economy.master.EconomyMasterData.ItemDef(
                1, "鉄", "個", "minecraft:iron_ingot", 10, 5, true, null, null, null);
        var potion = new com.ogatamizuki.economy.master.EconomyMasterData.ItemDef(
                2, "力", "個", "minecraft:potion", null, 40, true, "minecraft:strength", null, null);
        assertFalse(plain.hasVariantMatch());
        assertTrue(potion.hasVariantMatch());
        assertTrue(EconomyItemMatcher.hasVariantMatch(potion));
    }
}
