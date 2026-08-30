package com.ogatamizuki.lookalike;

import com.ogatamizuki.lookalike.LookalikeAttachments.ScanHistory;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class LookalikePlatform {
    public static Function<Player, ScanHistory> getScanHistory = player -> new ScanHistory();
    public static BiConsumer<Player, ScanHistory> setScanHistory = (player, history) -> {};

    public static Function<Player, Boolean> hasReceivedGuide = player -> false;
    public static BiConsumer<Player, Boolean> setReceivedGuide = (player, val) -> {};

    public static java.util.function.Predicate<String> isModLoadedCheck = modId -> false;

    private LookalikePlatform() {}

    public static boolean isModLoaded(String modId) {
        return isModLoadedCheck.test(modId);
    }

    public static ScanHistory getHistory(Player player) {
        return getScanHistory.apply(player);
    }

    public static void setHistory(Player player, ScanHistory history) {
        setScanHistory.accept(player, history);
    }

    public static boolean hasReceivedGuide(Player player) {
        return hasReceivedGuide.apply(player);
    }

    public static void setReceivedGuide(Player player, boolean received) {
        setReceivedGuide.accept(player, received);
    }
}
