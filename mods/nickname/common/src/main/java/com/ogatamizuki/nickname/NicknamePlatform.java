package com.ogatamizuki.nickname;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public final class NicknamePlatform {
    public static Consumer<ServerPlayer> refreshDisplayNames = player -> {};

    private NicknamePlatform() {}

    public static void refresh(ServerPlayer player) {
        refreshDisplayNames.accept(player);
    }
}
