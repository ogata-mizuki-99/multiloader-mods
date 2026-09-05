package com.ogatamizuki.economy.backend.local;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EconomyLocalShopServiceTest {

    @Test
    void mergeShopDetailChunks_combinesItemArrays() {
        String chunk1 = chunkJson(1, items(item(101, "iron"), item(102, "gold")));
        String chunk2 = chunkJson(1, items(item(103, "diamond")));

        String merged = EconomyLocalShopService.mergeShopDetailChunks(List.of(chunk1, chunk2));
        JsonObject root = JsonParser.parseString(merged).getAsJsonObject();

        assertEquals(1, root.get("shopId").getAsInt());
        assertEquals("Test Shop", root.get("shopName").getAsString());
        JsonArray items = root.getAsJsonArray("items");
        assertEquals(3, items.size());
        assertEquals(101, items.get(0).getAsJsonObject().get("id").getAsInt());
        assertEquals(103, items.get(2).getAsJsonObject().get("id").getAsInt());
    }

    @Test
    void mergeShopDetailChunks_singleChunkReturnsSameItems() {
        String chunk = chunkJson(5, items(item(1, "bone")));
        String merged = EconomyLocalShopService.mergeShopDetailChunks(List.of(chunk));
        assertEquals(1, JsonParser.parseString(merged).getAsJsonObject().getAsJsonArray("items").size());
    }

    private static String chunkJson(int shopId, JsonArray items) {
        JsonObject root = new JsonObject();
        root.addProperty("shopId", shopId);
        root.addProperty("shopName", "Test Shop");
        root.addProperty("npcType", "SELLER");
        root.add("items", items);
        return root.toString();
    }

    private static JsonArray items(JsonObject... entries) {
        JsonArray array = new JsonArray();
        for (JsonObject entry : entries) {
            array.add(entry);
        }
        return array;
    }

    private static JsonObject item(int id, String name) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("item_name", name);
        return obj;
    }
}
