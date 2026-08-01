package com.ogatamizuki.economy;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class EconomyNpc extends PathfinderMob {
    private static final EntityDataAccessor<String> NPC_TYPE = SynchedEntityData.defineId(EconomyNpc.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> SHOP_ID = SynchedEntityData.defineId(EconomyNpc.class, EntityDataSerializers.INT);

    public EconomyNpc(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(NPC_TYPE, "SELLER");
        builder.define(SHOP_ID, 1);
    }

    public String getNpcType() {
        return this.entityData.get(NPC_TYPE);
    }

    public void setNpcType(String type) {
        this.entityData.set(NPC_TYPE, type);
    }

    public int getShopId() {
        return this.entityData.get(SHOP_ID);
    }

    public void setShopId(int id) {
        this.entityData.set(SHOP_ID, id);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide()) {
            String type = getNpcType();
            int id = getShopId();
            EconomyMod.LOGGER.info("Interacted with EconomyNpc: type={}, shopId={} by {}", type, id, player.getName().getString());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("npc_type", getNpcType());
        tag.putInt("shop_id", getShopId());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setNpcType(tag.getStringOr("npc_type", "SELLER"));
        setShopId(tag.getIntOr("shop_id", 1));
    }
}
