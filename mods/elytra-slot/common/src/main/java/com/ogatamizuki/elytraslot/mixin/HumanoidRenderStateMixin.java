package com.ogatamizuki.elytraslot.mixin;

import com.ogatamizuki.elytraslot.HumanoidRenderStateExt;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(HumanoidRenderState.class)
public abstract class HumanoidRenderStateMixin implements HumanoidRenderStateExt {

    @Unique
    private ItemStack elytraSlotItem = ItemStack.EMPTY;

    @Override
    public ItemStack getElytraSlotItem() {
        return this.elytraSlotItem;
    }

    @Override
    public void setElytraSlotItem(ItemStack stack) {
        this.elytraSlotItem = stack;
    }
}
