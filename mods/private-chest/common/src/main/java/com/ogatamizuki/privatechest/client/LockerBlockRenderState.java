package com.ogatamizuki.privatechest.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public class LockerBlockRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public float openProgress = 0.0f;
}
