package com.ogatamizuki.economy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ogatamizuki.economy.EconomyMod;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** フリーマーケット出品データ（単体版）。 */
public final class EconomyFleaMarketSavedData extends SavedData {
    /**
     * 出品1件。ItemStack 本体は SavedData に直接載せない（ItemStack.CODEC はレジストリ依存で
     * 保存/読込が壊れやすい）。永続化は itemStackSnbt（SNBT 文字列）のみ。
     */
    public record Listing(
            String id,
            UUID sellerUuid,
            String sellerName,
            String itemKey,
            String itemName,
            int price,
            int quantity,
            int soldQuantity,
            String itemStackSnbt
    ) {
        public Listing {
            itemStackSnbt = itemStackSnbt == null ? "" : itemStackSnbt;
        }

        public static Listing create(
                String id,
                UUID sellerUuid,
                String sellerName,
                String itemKey,
                String itemName,
                int price,
                int quantity,
                int soldQuantity,
                HolderLookup.Provider registries,
                ItemStack itemStack
        ) {
            String snbt = FleaMarketStackCodec.encode(registries, FleaMarketStackCodec.template(itemStack));
            return new Listing(id, sellerUuid, sellerName, itemKey, itemName, price, quantity, soldQuantity, snbt);
        }

        public static final Codec<Listing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Listing::id),
                Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("sellerUuid").forGetter(Listing::sellerUuid),
                Codec.STRING.fieldOf("sellerName").forGetter(Listing::sellerName),
                Codec.STRING.fieldOf("itemKey").forGetter(Listing::itemKey),
                Codec.STRING.fieldOf("itemName").forGetter(Listing::itemName),
                Codec.INT.fieldOf("price").forGetter(Listing::price),
                Codec.INT.fieldOf("quantity").forGetter(Listing::quantity),
                Codec.INT.fieldOf("soldQuantity").forGetter(Listing::soldQuantity),
                Codec.STRING.optionalFieldOf("itemStackSnbt", "").forGetter(Listing::itemStackSnbt)
        ).apply(instance, Listing::new));

        public int remainingQuantity() {
            return quantity - soldQuantity;
        }

        public ItemStack resolveStack(HolderLookup.Provider registries) {
            return FleaMarketStackCodec.decode(registries, itemStackSnbt, itemKey, 1);
        }

        public ItemStack createGrantStack(HolderLookup.Provider registries, int count) {
            if (count <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack template = resolveStack(registries);
            if (template.isEmpty()) {
                return FleaMarketStackCodec.fromItemKey(itemKey, count);
            }
            return template.copyWithCount(count);
        }

        public Listing withSoldQuantity(int newSoldQuantity) {
            return new Listing(
                    id, sellerUuid, sellerName, itemKey, itemName, price, quantity, newSoldQuantity, itemStackSnbt
            );
        }
    }

    private static final Codec<List<Listing>> LISTINGS_CODEC = Listing.CODEC.listOf();

    private static final Codec<EconomyFleaMarketSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LISTINGS_CODEC.fieldOf("listings").forGetter(data -> data.listings)
    ).apply(instance, listings -> {
        EconomyFleaMarketSavedData data = new EconomyFleaMarketSavedData();
        data.listings.addAll(listings);
        return data;
    }));

    private static final SavedDataType<EconomyFleaMarketSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "flea_market_data"),
            EconomyFleaMarketSavedData::new,
            CODEC
    );

    private final List<Listing> listings = new ArrayList<>();

    public EconomyFleaMarketSavedData() {
    }

    public static EconomyFleaMarketSavedData get(MinecraftServer server) {
        ServerLevel level = server.overworld();
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Listing> activeListings() {
        return listings.stream().filter(l -> l.remainingQuantity() > 0).toList();
    }

    public Optional<Listing> find(String listingId) {
        return listings.stream().filter(l -> l.id().equals(listingId)).findFirst();
    }

    public void addListing(Listing listing) {
        listings.add(listing);
        setDirty();
    }

    public void updateListing(Listing listing) {
        for (int i = 0; i < listings.size(); i++) {
            if (listings.get(i).id().equals(listing.id())) {
                listings.set(i, listing);
                setDirty();
                return;
            }
        }
    }

    public void removeListing(String listingId) {
        listings.removeIf(l -> l.id().equals(listingId));
        setDirty();
    }

    public void clearAll() {
        listings.clear();
        setDirty();
    }
}
