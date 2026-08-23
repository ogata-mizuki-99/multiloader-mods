package com.ogatamizuki.economy;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.ogatamizuki.economy.master.EconomyMasterData;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;

import java.util.List;

/** ショップ設定に基づく NPC スポナーエッグ付与。 */
public final class EconomyNpcSpawnService {
    private EconomyNpcSpawnService() {
    }

    public static String giveForShop(ServerPlayer player, int shopId) {
        var server = player.level().getServer();
        if (server == null) {
            throw new IllegalStateException("サーバーが利用できません。");
        }
        EconomyMasterData.reload(server);
        var shop = EconomyMasterData.get().shop(shopId)
                .filter(EconomyMasterData.ShopDef::enabled)
                .orElseThrow(() -> new IllegalArgumentException("未知のショップ ID: " + shopId));
        if ("LOAN".equalsIgnoreCase(shop.npcType())) {
            giveLoanSpawnEgg(player, shopId, shop.shopName());
        } else {
            giveConfiguredSpawnEgg(player, shopId, shop.npcType(), shop.npcModel(), shop.shopName());
        }
        Component shopLabel = EconomyMasterI18n.shopNameComponent(shopId, shop.shopName());
        player.sendSystemMessage(Component.translatable("economy.chat.spawn_egg_given", shopLabel));
        return Component.translatable("economy.chat.spawn_egg_given", shopLabel).getString();
    }

    public static String giveAllForEnabledShops(ServerPlayer player) {
        var server = player.level().getServer();
        if (server == null) {
            throw new IllegalStateException("サーバーが利用できません。");
        }
        EconomyMasterData.reload(server);
        List<EconomyMasterData.ShopDef> shops = EconomyMasterData.get().allEnabledShops();
        if (shops.isEmpty()) {
            throw new IllegalStateException("有効なショップがありません。");
        }
        int count = 0;
        for (EconomyMasterData.ShopDef shop : shops) {
            if ("LOAN".equalsIgnoreCase(shop.npcType())) {
                giveLoanSpawnEgg(player, shop.id(), shop.shopName());
            } else {
                giveConfiguredSpawnEgg(player, shop.id(), shop.npcType(), shop.npcModel(), shop.shopName());
            }
            count++;
        }
        Component message = Component.translatable("economy.chat.spawn_eggs_all", count);
        player.sendSystemMessage(message);
        return message.getString();
    }

    /**
     * 頭上ネームをクライアント言語で解決できるよう、翻訳 Component をセットする。
     * 既存ワールドの日本語リテラル名も上書きする。
     */
    public static void applyLocalizedDisplayName(Entity entity, int shopId) {
        String fallback = EconomyMasterData.get().shop(shopId)
                .map(EconomyMasterData.ShopDef::shopName)
                .orElse("Shop " + shopId);
        entity.setCustomName(EconomyMasterI18n.shopNameComponent(shopId, fallback));
        entity.setCustomNameVisible(true);
    }

    /** スポーンエッグ EntityData 用: CustomName を translate JSON で書き込む。 */
    static String customNameNbtJson(int shopId, String shopNameFallback) {
        Component name = EconomyMasterI18n.shopNameComponent(shopId, shopNameFallback);
        JsonElement json = ComponentSerialization.CODEC
                .encodeStart(JsonOps.INSTANCE, name)
                .getOrThrow();
        return json.toString();
    }

    static void giveConfiguredSpawnEgg(ServerPlayer player, int shopId, String npcType, String npcModel, String shopName) {
        String baseModel = npcModel;
        String profession = null;
        if (npcModel.contains(":")) {
            String[] parts = npcModel.split(":");
            if (parts.length == 3) {
                baseModel = parts[0] + ":" + parts[1];
                profession = parts[2];
            }
        }

        Identifier entityTypeId = Identifier.parse(baseModel);
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityTypeId)
                .<EntityType<?>>map(Holder::value)
                .orElse(EntityType.VILLAGER);

        Identifier eggId = Identifier.fromNamespaceAndPath(entityTypeId.getNamespace(), entityTypeId.getPath() + "_spawn_egg");
        Item eggItem = BuiltInRegistries.ITEM.get(eggId)
                .map(Holder::value)
                .orElse(Items.VILLAGER_SPAWN_EGG);

        ItemStack eggStack = new ItemStack(eggItem);

        CompoundTag entityTag = new CompoundTag();
        CompoundTag forgeData = new CompoundTag();
        forgeData.putInt("shop_id", shopId);
        forgeData.putString("npc_type", npcType.toUpperCase());
        entityTag.put("ForgeData", forgeData);
        entityTag.putByte("NoAI", (byte) 1);
        entityTag.putByte("Invulnerable", (byte) 1);
        entityTag.putByte("PersistenceRequired", (byte) 1);
        entityTag.putByte("Silent", (byte) 1);

        net.minecraft.nbt.ListTag tags = new net.minecraft.nbt.ListTag();
        tags.add(net.minecraft.nbt.StringTag.valueOf("EconomyNPC:" + shopId + ":" + npcType.toUpperCase()));
        entityTag.put("Tags", tags);

        net.minecraft.nbt.ListTag handItems = new net.minecraft.nbt.ListTag();
        handItems.add(new CompoundTag());
        handItems.add(new CompoundTag());
        entityTag.put("HandItems", handItems);

        if (profession != null && baseModel.endsWith("villager")) {
            CompoundTag villagerData = new CompoundTag();
            villagerData.putString("profession", "minecraft:" + profession);
            villagerData.putInt("level", 1);
            villagerData.putString("type", "minecraft:plains");
            entityTag.put("VillagerData", villagerData);
        }

        entityTag.putString("CustomName", customNameNbtJson(shopId, shopName));
        entityTag.putByte("CustomNameVisible", (byte) 1);

        eggStack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(entityType, entityTag));
        eggStack.set(DataComponents.CUSTOM_NAME, eggDisplayName(shopId, shopName));

        if (!player.getInventory().add(eggStack)) {
            player.drop(eggStack, false);
        }
    }

    static void giveLoanSpawnEgg(ServerPlayer player, int shopId, String shopName) {
        ItemStack eggStack = new ItemStack(EconomyMod.LOAN_NPC_SPAWN_EGG.get());

        CompoundTag entityTag = new CompoundTag();
        CompoundTag forgeData = new CompoundTag();
        forgeData.putInt("shop_id", shopId);
        forgeData.putString("npc_type", "LOAN");
        entityTag.put("ForgeData", forgeData);
        entityTag.putByte("NoAI", (byte) 1);
        entityTag.putByte("Invulnerable", (byte) 1);
        entityTag.putByte("PersistenceRequired", (byte) 1);
        entityTag.putByte("Silent", (byte) 1);
        entityTag.putString("CustomName", customNameNbtJson(shopId, shopName));
        entityTag.putByte("CustomNameVisible", (byte) 1);

        net.minecraft.nbt.ListTag handItems = new net.minecraft.nbt.ListTag();
        handItems.add(new CompoundTag());
        handItems.add(new CompoundTag());
        entityTag.put("HandItems", handItems);

        eggStack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(EconomyMod.LOAN_NPC.get(), entityTag));
        eggStack.set(DataComponents.CUSTOM_NAME, eggDisplayName(shopId, shopName));

        if (!player.getInventory().add(eggStack)) {
            player.drop(eggStack, false);
        }
    }

    private static Component eggDisplayName(int shopId, String shopNameFallback) {
        return Component.empty()
                .append(EconomyMasterI18n.shopNameComponent(shopId, shopNameFallback))
                .append(Component.literal(" [ID: " + shopId + "]"));
    }
}
