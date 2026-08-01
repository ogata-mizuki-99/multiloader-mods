package com.ogatamizuki.economy.master;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyMasterDataEditsTest {

    private static JsonObject minimalManifest() {
        return JsonParser.parseString("""
                {
                  "shops": [{"id": 1, "shopName": "Test", "npcType": "SELLER", "enabled": true}],
                  "items": [{"id": 1, "name": "Gold", "itemKey": "economy:gold_coin", "unit": "枚", "enabled": true}],
                  "shopItems": [],
                  "actionRewards": [],
                  "etfItems": []
                }
                """).getAsJsonObject();
    }

    @Test
    void applyManifestEdits_createsActionReward() {
        JsonObject manifest = minimalManifest();
        JsonArray edits = new JsonArray();
        JsonObject edit = new JsonObject();
        edit.addProperty("create", true);
        edit.addProperty("actionType", "TEST_ACTION");
        edit.addProperty("displayName", "テスト");
        edit.addProperty("rewardAmount", 42);
        edits.add(edit);

        EconomyMasterData.applyManifestEdits(manifest, edits, "actionRewards");

        JsonObject row = manifest.getAsJsonArray("actionRewards").get(0).getAsJsonObject();
        assertEquals("TEST_ACTION", row.get("actionType").getAsString());
        assertEquals("テスト", row.get("displayName").getAsString());
        assertEquals(42, row.get("rewardAmount").getAsInt());
        assertTrue(row.get("enabled").getAsBoolean());
    }

    @Test
    void applyManifestEdits_createsItemWithAutoId() {
        JsonObject manifest = minimalManifest();
        JsonArray edits = new JsonArray();
        JsonObject edit = new JsonObject();
        edit.addProperty("create", true);
        edit.addProperty("name", "石");
        edit.addProperty("itemKey", "minecraft:stone");
        edit.addProperty("buyPrice", 10);
        edits.add(edit);

        EconomyMasterData.applyManifestEdits(manifest, edits, "items");

        JsonObject row = manifest.getAsJsonArray("items").get(1).getAsJsonObject();
        assertEquals(2, row.get("id").getAsInt());
        assertEquals("石", row.get("name").getAsString());
        assertEquals("minecraft:stone", row.get("itemKey").getAsString());
        assertEquals(10, row.get("buyPrice").getAsInt());
    }

    @Test
    void applyManifestEdits_createsShopItem() {
        JsonObject manifest = minimalManifest();
        JsonArray edits = new JsonArray();
        JsonObject edit = new JsonObject();
        edit.addProperty("create", true);
        edit.addProperty("shopId", 1);
        edit.addProperty("itemId", 1);
        edit.addProperty("dailyLimit", 5);
        edits.add(edit);

        EconomyMasterData.applyManifestEdits(manifest, edits, "shopItems");

        JsonObject row = manifest.getAsJsonArray("shopItems").get(0).getAsJsonObject();
        assertEquals(1, row.get("shopId").getAsInt());
        assertEquals(1, row.get("itemId").getAsInt());
        assertEquals(1, row.get("orderNo").getAsInt());
        assertEquals(5, row.get("dailyLimit").getAsInt());
    }

    @Test
    void applyManifestEdits_createsEtfItem() {
        JsonObject manifest = minimalManifest();
        JsonArray edits = new JsonArray();
        JsonObject edit = new JsonObject();
        edit.addProperty("create", true);
        edit.addProperty("etfCode", "テストETF");
        edit.addProperty("itemKey", "minecraft:bone");
        edit.addProperty("influenceWeight", 0.05);
        edits.add(edit);

        EconomyMasterData.applyManifestEdits(manifest, edits, "etfItems");

        JsonObject row = manifest.getAsJsonArray("etfItems").get(0).getAsJsonObject();
        assertEquals("テストETF", row.get("etfCode").getAsString());
        assertEquals("minecraft:bone", row.get("itemKey").getAsString());
        assertEquals(0.05, row.get("influenceWeight").getAsDouble(), 0.0001);
    }
}
