package com.ogatamizuki.sleep.neoforge;

import com.ogatamizuki.sleep.DaySleepTimeAdvance;
import com.ogatamizuki.sleep.SleepClientFlagsPayload;
import com.ogatamizuki.sleep.SleepCommon;
import com.ogatamizuki.sleep.SleepCommonConfigPushPayload;
import com.ogatamizuki.sleep.SleepWakeFullHeal;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.ClockAdjustment;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod(SleepCommon.MODID)
public class SleepModNeoForge {
    private static final int VANILLA_SLEEPING_PERCENTAGE = 100;
    private static final Map<UUID, Integer> DAY_SLEEP_TICKS = new ConcurrentHashMap<>();

    public SleepModNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        SleepCommon.LOGGER.info("Good Sleep Mod (NeoForge) Initializing...");

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(this::onConfigReload);
        modEventBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.register(this);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(SleepClientFlagsPayload.TYPE, SleepClientFlagsPayload.STREAM_CODEC);
        registrar.playToServer(
                SleepCommonConfigPushPayload.TYPE,
                SleepCommonConfigPushPayload.STREAM_CODEC,
                this::handleCommonConfigPush);
    }

    private void handleCommonConfigPush(SleepCommonConfigPushPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (!serverPlayer.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("good_sleep.configuration.push_denied")
                                .withStyle(ChatFormatting.RED));
                return;
            }

            int healInterval = Math.max(0, Math.min(200, payload.healIntervalTicks()));
            Config.ALLOW_DAY_SLEEP.set(payload.allowDaySleep());
            Config.ALLOW_DAY_SLEEP.save();
            Config.HEAL_WHILE_SLEEPING.set(payload.healWhileSleeping());
            Config.HEAL_WHILE_SLEEPING.save();
            Config.HEAL_INTERVAL_TICKS.set(healInterval);
            Config.HEAL_INTERVAL_TICKS.save();
            Config.ONE_PLAYER_SKIP.set(payload.onePlayerSkip());
            Config.ONE_PLAYER_SKIP.save();
            Config.syncToCommon();

            MinecraftServer server = serverPlayer.level().getServer();
            if (server != null) {
                applySleepingPercentage(server);
            }
            syncClientFlagsToAllPlayers();

            SleepCommon.LOGGER.info(
                    "Good Sleep common config pushed by {}: allowDaySleep={}, healWhileSleeping={}, healIntervalTicks={}, onePlayerSkip={}",
                    serverPlayer.getGameProfile().name(),
                    Config.ALLOW_DAY_SLEEP.get(),
                    Config.HEAL_WHILE_SLEEPING.get(),
                    Config.HEAL_INTERVAL_TICKS.get(),
                    Config.ONE_PLAYER_SKIP.get());
            serverPlayer.sendSystemMessage(
                    Component.translatable("good_sleep.configuration.push_ok")
                            .withStyle(ChatFormatting.GREEN));
        });
    }

    public static void syncClientFlagsToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, SleepClientFlagsPayload.fromConfig());
    }

    public static void syncClientFlagsToAllPlayers() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        PacketDistributor.sendToAllPlayers(SleepClientFlagsPayload.fromConfig());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncClientFlagsToPlayer(player);
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        Config.syncToCommon();
        applySleepingPercentage(event.getServer());
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == Config.SPEC) {
            Config.syncToCommon();
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                applySleepingPercentage(server);
                syncClientFlagsToAllPlayers();
            }
        }
    }

    @SubscribeEvent
    public void onSleepFinished(SleepFinishedTimeEvent event) {
        if (isWerewolfActiveNight()) {
            event.setCanceled(true);
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // 深い睡眠完了（時刻スキップ）時に全快
        if (Config.HEAL_WHILE_SLEEPING.get()) {
            for (ServerPlayer player : level.players()) {
                if (player.isSleeping()) {
                    SleepWakeFullHeal.markDeepSleepReached(player);
                    SleepWakeFullHeal.fullHealIfEnabled(player);
                }
            }
        }

        if (!Config.ALLOW_DAY_SLEEP.get()) {
            return;
        }
        if (level.isDarkOutside()) {
            return;
        }

        // バニラは朝へ戻すが、仕様どおり夜へ進める
        event.setAdjustment(new ClockAdjustment.Marker(ClockTimeMarkers.NIGHT));
        DAY_SLEEP_TICKS.clear();
    }

    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DAY_SLEEP_TICKS.remove(player.getUUID());
        if (player.level().isClientSide()) {
            return;
        }
        // 深い睡眠到達後の起床のみ全快（途中でベッドを出た場合はフラグ無し）
        SleepWakeFullHeal.onServerPlayerTick(player);
    }

    private boolean isWerewolfActiveNight() {
        if (!net.neoforged.fml.ModList.get().isLoaded("werewolf")) {
            return false;
        }
        try {
            Class<?> managerClass = Class.forName("com.ogatamizuki.werewolf.game.WerewolfGameManager");
            java.lang.reflect.Method getDataMethod = managerClass.getMethod("getData");
            Object data = getDataMethod.invoke(null);
            if (data == null) {
                return false;
            }
            java.lang.reflect.Field phaseField = data.getClass().getField("phase");
            java.lang.reflect.Field modeField = data.getClass().getField("mode");
            Object phase = phaseField.get(data);
            Object mode = modeField.get(data);
            
            Class<?> phasesClass = Class.forName("com.ogatamizuki.werewolf.game.WerewolfPhases");
            java.lang.reflect.Method isActiveNightMethod = phasesClass.getMethod("isActiveNight", phase.getClass(), String.class);
            return (boolean) isActiveNightMethod.invoke(null, phase, (String) mode);
        } catch (Exception e) {
            SleepCommon.LOGGER.error("Failed to check werewolf active night state via reflection", e);
            return false;
        }
    }

    @SubscribeEvent
    public void onCanPlayerSleep(CanPlayerSleepEvent event) {
        if (!Config.ALLOW_DAY_SLEEP.get() || event.getProblem() == null) {
            return;
        }

        Player.BedSleepingProblem vanilla = event.getVanillaProblem();
        if (vanilla == Player.BedSleepingProblem.NOT_SAFE
                || vanilla == Player.BedSleepingProblem.TOO_FAR_AWAY
                || vanilla == Player.BedSleepingProblem.OBSTRUCTED) {
            return;
        }

        if (isDaytimeSleepBlocked(event.getEntity(), event.getPos())) {
            event.setProblem(null);
        }
    }

    @SubscribeEvent
    public void onCanContinueSleeping(CanContinueSleepingEvent event) {
        if (!Config.ALLOW_DAY_SLEEP.get() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        BlockPos pos = player.getSleepingPos().orElse(player.blockPosition());
        if (isDaytimeSleepBlocked(player, pos)) {
            event.setContinueSleeping(true);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 昼間睡眠: SleepFinishedTimeEvent が飛ばない場合のフォールバック
        if (Config.ALLOW_DAY_SLEEP.get() && player.isSleeping()
                && player.level() instanceof ServerLevel level
                && DaySleepTimeAdvance.isDaytime(level)) {
            int ticks = DAY_SLEEP_TICKS.merge(player.getUUID(), 1, Integer::sum);
            if (ticks >= DaySleepTimeAdvance.SLEEP_TICKS_REQUIRED) {
                DaySleepTimeAdvance.finishDaySleep(level);
                DAY_SLEEP_TICKS.clear();
                return;
            }
        } else {
            DAY_SLEEP_TICKS.remove(player.getUUID());
        }

        // 深い睡眠到達の記録 / 起床時全快
        SleepWakeFullHeal.onServerPlayerTick(player);

        if (!Config.HEAL_WHILE_SLEEPING.get() || !player.isSleeping()) {
            return;
        }

        int interval = Config.HEAL_INTERVAL_TICKS.get();
        if (interval == 0) {
            if (player.getHealth() < player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
            return;
        }

        // Do not require isSleepingLongEnough(): day-sleep / one-player skip often finishes
        // at or before that threshold, so heal would never run.
        if (player.tickCount % interval != 0) {
            return;
        }

        if (player.getHealth() < player.getMaxHealth()) {
            player.heal(1.0F);
        }
    }

    private static boolean isDaytimeSleepBlocked(Player player, BlockPos pos) {
        Level level = player.level();
        BedRule rule = level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, pos);
        return !rule.canSleep(level);
    }

    private static void applySleepingPercentage(MinecraftServer server) {
        int percentage = Config.ONE_PLAYER_SKIP.get() ? 0 : VANILLA_SLEEPING_PERCENTAGE;
        server.getGameRules().set(GameRules.PLAYERS_SLEEPING_PERCENTAGE, percentage, server);
        SleepCommon.LOGGER.info("playersSleepingPercentage set to {}", percentage);
    }
}
