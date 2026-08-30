package com.ogatamizuki.lookalike;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import com.ogatamizuki.lookalike.LookalikeAttachments.ScanEntry;

import java.util.List;
import java.util.UUID;

public class NetworkPayloads {

    public record DisguiseRequestPayload(String targetUuidStr) implements CustomPacketPayload {
        public static final Type<DisguiseRequestPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(LookalikeCommon.MODID, "disguise_request"));

        public static final StreamCodec<ByteBuf, DisguiseRequestPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, DisguiseRequestPayload::targetUuidStr,
                DisguiseRequestPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ScanHistorySyncPayload(List<ScanEntry> entries) implements CustomPacketPayload {
        public static final Type<ScanHistorySyncPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(LookalikeCommon.MODID, "scan_history_sync"));

        public static final StreamCodec<ByteBuf, ScanEntry> SCAN_ENTRY_STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, e -> e.uuid().toString(),
                ByteBufCodecs.STRING_UTF8, ScanEntry::name,
                (uuidStr, name) -> new ScanEntry(UUID.fromString(uuidStr), name)
        );

        public static final StreamCodec<ByteBuf, ScanHistorySyncPayload> STREAM_CODEC = StreamCodec.composite(
                SCAN_ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list()), ScanHistorySyncPayload::entries,
                ScanHistorySyncPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DisguiseSkinEntry(
            String uuidStr,
            String textureValue,
            PlayerSkin.Patch skinPatch,
            String targetUuidStr
    ) {}

    public record DisguiseListSyncPayload(List<DisguiseSkinEntry> disguisedPlayers) implements CustomPacketPayload {
        public static final Type<DisguiseListSyncPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(LookalikeCommon.MODID, "disguise_list_sync"));

        public static final StreamCodec<ByteBuf, DisguiseSkinEntry> SKIN_ENTRY_STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, DisguiseSkinEntry::uuidStr,
                ByteBufCodecs.STRING_UTF8, DisguiseSkinEntry::textureValue,
                PlayerSkin.Patch.STREAM_CODEC, DisguiseSkinEntry::skinPatch,
                ByteBufCodecs.STRING_UTF8, DisguiseSkinEntry::targetUuidStr,
                DisguiseSkinEntry::new
        );

        public static final StreamCodec<ByteBuf, DisguiseListSyncPayload> STREAM_CODEC = StreamCodec.composite(
                SKIN_ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list()), DisguiseListSyncPayload::disguisedPlayers,
                DisguiseListSyncPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ShadowPathEntry(
            String actorUuidStr,
            String hostUuidStr,
            int fromX,
            int fromY,
            int fromZ,
            int toX,
            int toY,
            int toZ
    ) {}

    public record ShadowAppearanceSyncPayload(
            List<String> shadowPlayerUuids,
            List<ShadowPathEntry> paths,
            boolean pathVisualizationEnabled
    ) implements CustomPacketPayload {
        public static final Type<ShadowAppearanceSyncPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(LookalikeCommon.MODID, "shadow_appearance_sync"));

        public static final StreamCodec<ByteBuf, ShadowPathEntry> PATH_ENTRY_STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ShadowPathEntry::actorUuidStr,
                ByteBufCodecs.STRING_UTF8, ShadowPathEntry::hostUuidStr,
                ByteBufCodecs.VAR_INT, ShadowPathEntry::fromX,
                ByteBufCodecs.VAR_INT, ShadowPathEntry::fromY,
                ByteBufCodecs.VAR_INT, ShadowPathEntry::fromZ,
                ByteBufCodecs.VAR_INT, ShadowPathEntry::toX,
                ByteBufCodecs.VAR_INT, ShadowPathEntry::toY,
                ByteBufCodecs.VAR_INT, ShadowPathEntry::toZ,
                ShadowPathEntry::new
        );

        public static final StreamCodec<ByteBuf, ShadowAppearanceSyncPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ShadowAppearanceSyncPayload::shadowPlayerUuids,
                PATH_ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list()), ShadowAppearanceSyncPayload::paths,
                ByteBufCodecs.BOOL, ShadowAppearanceSyncPayload::pathVisualizationEnabled,
                ShadowAppearanceSyncPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ScanHistoryActionPayload(byte action, String argument) implements CustomPacketPayload {
        public static final byte ACTION_DELETE = 0;

        public static final Type<ScanHistoryActionPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(LookalikeCommon.MODID, "scan_history_action"));

        public static final StreamCodec<ByteBuf, ScanHistoryActionPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BYTE, ScanHistoryActionPayload::action,
                ByteBufCodecs.STRING_UTF8, ScanHistoryActionPayload::argument,
                ScanHistoryActionPayload::new
        );

        public static ScanHistoryActionPayload delete(String uuidStr) {
            return new ScanHistoryActionPayload(ACTION_DELETE, uuidStr);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
