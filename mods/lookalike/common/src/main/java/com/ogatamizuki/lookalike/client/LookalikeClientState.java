package com.ogatamizuki.lookalike.client;

import com.ogatamizuki.lookalike.LookalikeAttachments.ScanEntry;
import com.ogatamizuki.lookalike.LookalikeClientFlags;
import com.ogatamizuki.lookalike.LookalikeClientFlagsPayload;
import com.ogatamizuki.lookalike.NetworkPayloads;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LookalikeClientState {
    private static final List<ScanEntry> scanHistory = new ArrayList<>();
    private static final Set<UUID> disguisedUuids = ConcurrentHashMap.newKeySet();
    private static boolean scanScreenOpen = false;

    private LookalikeClientState() {}

    public static List<ScanEntry> getScanHistory() {
        return Collections.unmodifiableList(scanHistory);
    }

    public static boolean isDisguised(UUID uuid) {
        return disguisedUuids.contains(uuid);
    }

    public static void onScanScreenOpened() {
        scanScreenOpen = true;
    }

    public static void onScanScreenClosed() {
        scanScreenOpen = false;
    }

    public static boolean isScanScreenOpen() {
        return scanScreenOpen;
    }

    public static void applyScanHistorySync(NetworkPayloads.ScanHistorySyncPayload payload) {
        scanHistory.clear();
        scanHistory.addAll(payload.entries());
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof ScanHistoryEditScreen editScreen) {
            editScreen.updateEntries(scanHistory);
        }
    }

    public static void applyDisguiseListSync(NetworkPayloads.DisguiseListSyncPayload payload) {
        disguisedUuids.clear();
        for (NetworkPayloads.DisguiseSkinEntry entry : payload.disguisedPlayers()) {
            disguisedUuids.add(UUID.fromString(entry.uuidStr()));
        }
    }

    public static void applyClientFlags(LookalikeClientFlagsPayload payload) {
        LookalikeClientFlags.apply(payload);
    }

    public static void clear() {
        scanHistory.clear();
        disguisedUuids.clear();
        LookalikeClientFlags.clear();
    }
}
