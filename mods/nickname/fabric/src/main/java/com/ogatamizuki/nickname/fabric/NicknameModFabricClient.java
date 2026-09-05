package com.ogatamizuki.nickname.fabric;

import com.ogatamizuki.nickname.NicknameClearAllPayload;
import com.ogatamizuki.nickname.NicknameStorage;
import com.ogatamizuki.nickname.NicknameSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class NicknameModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NicknameModFabric.LOGGER.info("Nickname Mod (Fabric Client) Initializing...");

        ClientPlayNetworking.registerGlobalReceiver(NicknameSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> NicknameStorage.setNickname(payload.playerUuid(), payload.nickname()));
        });

        ClientPlayNetworking.registerGlobalReceiver(NicknameClearAllPayload.TYPE, (payload, context) -> {
            context.client().execute(NicknameStorage::clear);
        });
    }
}
