package net.dalerd.backroomsbounded.advancement;

import net.dalerd.backroomsbounded.BackroomsBounded;
import net.dalerd.backroomsbounded.entity.ModEntities;
import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AdvancementTriggerHandler {

    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20; // Check every second

    // Track office ring entry
    private static final Map<UUID, Boolean> wasInOffice = new HashMap<>();

    // Track deep office time (for 4500-7500 blocks)
    private static final Map<UUID, Integer> deepOfficeTimer = new HashMap<>();
    private static final int DEEP_OFFICE_TIME_REQUIRED = 60; // 60 seconds = 1200 ticks

    // Track bacteria shroom detections
    private static final Map<UUID, Integer> shroomDetections = new HashMap<>();

    // Track mimic proximity
    private static final Map<UUID, Boolean> mimicSeen = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            if (tickCounter % CHECK_INTERVAL != 0) return;

            for (ServerWorld world : server.getWorlds()) {
                if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) continue;

                for (ServerPlayerEntity player : world.getPlayers()) {
                    checkAdvancements(player);
                }
            }
        });
    }

    private static void checkAdvancements(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        BlockPos pos = player.getBlockPos();

        // === ENTER OFFICE ===
        // Check if player is in office zone (rings 3000-6000, 9000-12000, etc.)
        if (isInOfficeZone(player)) {
            if (!wasInOffice.getOrDefault(uuid, false)) {
                ModAdvancements.grant(player, ModAdvancements.ENTER_OFFICE);
                wasInOffice.put(uuid, true);
            }
        } else {
            wasInOffice.put(uuid, false);
        }

        // === DEEP OFFICE EXPLORER ===
        if (isInOfficeZone(player) && isInDeepOffice(player)) {
            int time = deepOfficeTimer.getOrDefault(uuid, 0) + 1;
            deepOfficeTimer.put(uuid, time);
            if (time >= DEEP_OFFICE_TIME_REQUIRED) {
                ModAdvancements.grant(player, ModAdvancements.DEEP_OFFICE_EXPLORER);
            }
        } else {
            deepOfficeTimer.put(uuid, 0);
        }

        // === MIMIC PROXIMITY (8 blocks) ===
        if (!mimicSeen.getOrDefault(uuid, false)) {
            boolean mimicNearby = player.getWorld().getEntitiesByType(ModEntities.MIMIC,
                    player.getBoundingBox().expand(8), e -> true).size() > 0;
            if (mimicNearby) {
                mimicSeen.put(uuid, true);
            }
        }
    }

    private static boolean isInOfficeZone(ServerPlayerEntity player) {
        int blockX = player.getBlockX();
        int blockZ = player.getBlockZ();
        double distance = Math.sqrt(blockX * blockX + blockZ * blockZ);

        // Office zones: 3000-6000, 9000-12000, 15000-18000, etc.
        double ringPosition = distance % 12000;
        return ringPosition >= 6000; // Office is 6000-12000 in each cycle
    }

    private static boolean isInDeepOffice(ServerPlayerEntity player) {
        int blockX = player.getBlockX();
        int blockZ = player.getBlockZ();
        double distance = Math.sqrt(blockX * blockX + blockZ * blockZ);

        // Deep office: 4500-7500 blocks from origin WITHIN the office ring
        // Since office starts at 6000, deep office is at absolute 10500-13500 in ring terms
        double ringPosition = distance % 12000;
        return ringPosition >= 6000 && ringPosition <= 12000 && distance >= 4500 && distance <= 7500;
    }

    // === METHODS TO CALL FROM YOUR EVENT HANDLERS ===

    /**
     * Call this from BacteriaShroomDetectionHandler when a player gets 3 strikes
     */
    public static void onBacteriaShroomTriggered(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        int detections = shroomDetections.getOrDefault(uuid, 0) + 1;
        shroomDetections.put(uuid, detections);

        if (detections >= 3) {
            ModAdvancements.grant(player, ModAdvancements.TRIGGER_BACTERIA_SHROOM);
        }
    }

    /**
     * Call this from BacteriumEntity when a player survives a grab
     */
    public static void onSurviveBacteriumGrab(ServerPlayerEntity player) {
        ModAdvancements.grant(player, ModAdvancements.SURVIVE_BACTERIUM_GRAB);
    }

    /**
     * Call this from SanityEvents when panic reaches 90+
     */
    public static void onPanicAttack(ServerPlayerEntity player) {
        ModAdvancements.grant(player, ModAdvancements.PANIC_ATTACK);
    }

    /**
     * Call this from BlockGlitchHandler when player uses glitch block but stays in backrooms
     */
    public static void onFailedEscape(ServerPlayerEntity player) {
        ModAdvancements.grant(player, ModAdvancements.FAILED_ESCAPE);
    }
}