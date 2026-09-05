package com.ogatamizuki.economy.backend.local;

import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ogatamizuki.economy.data.EconomyFleaMarketSavedData;
import com.ogatamizuki.economy.data.EconomyWorldSavedData;
import com.ogatamizuki.economy.data.FleaMarketStackCodec;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

/** フリーマーケットのローカル処理。 */
public final class EconomyLocalFleaMarketService {
    private EconomyLocalFleaMarketService() {
    }

    public static String fetchListings() {
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return null;
        }
        JsonArray array = new JsonArray();
        for (EconomyFleaMarketSavedData.Listing listing : EconomyFleaMarketSavedData.get(server).activeListings()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", listing.id());
            obj.addProperty("sellerUuid", listing.sellerUuid().toString());
            obj.addProperty("sellerName", listing.sellerName());
            obj.addProperty("itemKey", listing.itemKey());
            obj.addProperty("itemName", listing.itemName());
            obj.addProperty("price", listing.price());
            obj.addProperty("remainingQuantity", listing.remainingQuantity());
            String stackNbt = listing.itemStackSnbt();
            if (stackNbt == null || stackNbt.isBlank()) {
                stackNbt = FleaMarketStackCodec.encode(server.registryAccess(), listing.resolveStack(server.registryAccess()));
            }
            if (!stackNbt.isEmpty()) {
                obj.addProperty("itemStackNbt", stackNbt);
            }
            array.add(obj);
        }
        return array.toString();
    }

    public static JsonObject listItem(
            UUID sellerUuid,
            String sellerName,
            String itemKey,
            String itemName,
            int price,
            int quantity,
            ItemStack itemStack
    ) {
        if (price <= 0 || quantity <= 0) {
            return error("Invalid parameters");
        }
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return error("World data unavailable");
        }
        EconomyWorldSavedData.get(server).getOrCreate(sellerUuid, sellerName);
        String listingId = UUID.randomUUID().toString();
        EconomyFleaMarketSavedData.Listing listing = EconomyFleaMarketSavedData.Listing.create(
                listingId,
                sellerUuid,
                sellerName,
                itemKey,
                itemName,
                price,
                quantity,
                0,
                server.registryAccess(),
                itemStack
        );
        EconomyFleaMarketSavedData.get(server).addListing(listing);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("listingId", listingId);
        result.addProperty("message", "出品しました。");
        return result;
    }

    public static JsonObject buy(UUID buyerUuid, String listingId, int quantity) {
        if (quantity <= 0) {
            return error("Invalid parameters");
        }
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return error("World data unavailable");
        }
        EconomyFleaMarketSavedData fleaData = EconomyFleaMarketSavedData.get(server);
        var listingOpt = fleaData.find(listingId);
        if (listingOpt.isEmpty()) {
            return error("Listing not found");
        }
        EconomyFleaMarketSavedData.Listing listing = listingOpt.get();
        int remaining = listing.remainingQuantity();
        if (remaining < quantity) {
            return error(String.format("在庫が不足しています (残り: %d個)", remaining));
        }
        if (listing.sellerUuid().equals(buyerUuid)) {
            return error("自分の出品は購入できません。");
        }

        int totalPrice = listing.price() * quantity;
        EconomyWorldSavedData worldData = EconomyWorldSavedData.get(server);
        EconomyWorldSavedData.PlayerRecord buyer = worldData.getOrCreate(buyerUuid, "");
        if (buyer.balance() < totalPrice) {
            return error("所持金が不足しています。");
        }

        EconomyWorldSavedData.PlayerRecord seller = worldData.getOrCreate(listing.sellerUuid(), listing.sellerName());
        EconomyWorldSavedData.PlayerRecord updatedBuyer = copyWithBalance(buyer, buyer.balance() - totalPrice);
        EconomyWorldSavedData.PlayerRecord updatedSeller = copyWithBalance(seller, seller.balance() + totalPrice);
        worldData.putPlayer(buyerUuid, updatedBuyer);
        worldData.putPlayer(listing.sellerUuid(), updatedSeller);

        fleaData.updateListing(listing.withSoldQuantity(listing.soldQuantity() + quantity));

        ItemStack grant = listing.createGrantStack(server.registryAccess(), quantity);
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("itemKey", listing.itemKey());
        result.addProperty("itemName", listing.itemName());
        result.addProperty("newBalance", updatedBuyer.balance());
        result.addProperty("sellerUuid", listing.sellerUuid().toString());
        result.addProperty("sellerNewBalance", updatedSeller.balance());
        result.addProperty("totalPrice", totalPrice);
        result.addProperty("message", listing.itemName() + "を" + quantity + "個購入しました。");
        String stackNbt = listing.itemStackSnbt();
        if (stackNbt == null || stackNbt.isBlank()) {
            stackNbt = FleaMarketStackCodec.encode(server.registryAccess(), grant);
        }
        if (!stackNbt.isEmpty()) {
            result.addProperty("itemStackNbt", stackNbt);
        }
        result.addProperty("grantQuantity", quantity);
        return result;
    }

    public static JsonObject cancel(UUID sellerUuid, String listingId) {
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return error("World data unavailable");
        }
        EconomyFleaMarketSavedData fleaData = EconomyFleaMarketSavedData.get(server);
        var listingOpt = fleaData.find(listingId);
        if (listingOpt.isEmpty()) {
            return error("Listing not found");
        }
        EconomyFleaMarketSavedData.Listing listing = listingOpt.get();
        if (!listing.sellerUuid().equals(sellerUuid)) {
            return error("権限がありません。");
        }
        int remaining = listing.remainingQuantity();
        fleaData.removeListing(listingId);

        ItemStack grant = listing.createGrantStack(server.registryAccess(), remaining);
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("itemKey", listing.itemKey());
        result.addProperty("itemName", listing.itemName());
        result.addProperty("remainingQuantity", remaining);
        result.addProperty("message", "出品を取り消しました。");
        String stackNbt = listing.itemStackSnbt();
        if (stackNbt == null || stackNbt.isBlank()) {
            stackNbt = FleaMarketStackCodec.encode(server.registryAccess(), grant);
        }
        if (!stackNbt.isEmpty()) {
            result.addProperty("itemStackNbt", stackNbt);
        }
        return result;
    }

    private static EconomyWorldSavedData.PlayerRecord copyWithBalance(EconomyWorldSavedData.PlayerRecord record, int balance) {
        return new EconomyWorldSavedData.PlayerRecord(
                record.username(), balance, record.bankBalance(), record.debt(),
                record.totalEarnings(), record.totalLost(),
                record.etfBuyAmount(), record.etfShortAmount(), record.etfProfitAmount(),
                record.totalTradeCount()
        );
    }

    private static JsonObject error(String message) {
        JsonObject result = new JsonObject();
        result.addProperty("success", false);
        result.addProperty("error", message);
        return result;
    }
}
