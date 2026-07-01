package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.advancement.AdvancementManager;
import net.dalerd.backroomsbounded.block.ModBlocks;
import net.dalerd.backroomsbounded.entity.ModEntities;
import net.dalerd.backroomsbounded.entity.bacterium.BacteriumAI;
import net.dalerd.backroomsbounded.entity.bacterium.BacteriumEntity;
import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.*;

public class BacteriaShroomDetectionHandler {

    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20;
    private static final int BASE_DETECTION_RANGE = 20;
    private static final int COOLDOWN_TICKS = 100;
    private static final int STRIKE_WINDOW_TICKS = 1200;
    private static final int STRIKES_TO_ALERT = 3;

    private static final Map<BlockPos, Integer> shroomCooldowns = new HashMap<>();
    private static final Map<BlockPos, StrikeTracker> strikeTrackers = new HashMap<>();
    private static final Map<ServerPlayerEntity, Integer> noisyPlayers = new HashMap<>();

    // Track shroom detections per player for advancement
    private static final Map<UUID, Integer> playerShroomDetections = new HashMap<>();

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient) return;
            if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) return;
            if (player instanceof ServerPlayerEntity sp) {
                markPlayerNoisy(sp, 40);
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) return ActionResult.PASS;
            if (player instanceof ServerPlayerEntity sp) {
                markPlayerNoisy(sp, 40);
            }
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(ItemStack.EMPTY);
            if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) return TypedActionResult.pass(ItemStack.EMPTY);
            if (player instanceof ServerPlayerEntity sp) {
                markPlayerNoisy(sp, 40);
            }
            return TypedActionResult.pass(ItemStack.EMPTY);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            shroomCooldowns.entrySet().removeIf(entry -> entry.getValue() <= 0);
            shroomCooldowns.replaceAll((pos, cooldown) -> cooldown - 1);

            noisyPlayers.entrySet().removeIf(entry -> entry.getValue() <= 0);
            noisyPlayers.replaceAll((player, time) -> time - 1);

            strikeTrackers.entrySet().removeIf(entry ->
                    (tickCounter - entry.getValue().firstStrikeTime) > STRIKE_WINDOW_TICKS);

            if (tickCounter % CHECK_INTERVAL != 0) return;

            for (ServerWorld world : server.getWorlds()) {
                if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) continue;

                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (player.isCreative() || player.isSpectator()) continue;
                    if (!isPlayerNoisy(player)) continue;

                    checkShroomsNearPlayer(world, player);
                }
            }
        });
    }

    private static void markPlayerNoisy(ServerPlayerEntity player, int ticks) {
        noisyPlayers.put(player, Math.max(noisyPlayers.getOrDefault(player, 0), ticks));
    }

    private static boolean isPlayerNoisy(ServerPlayerEntity player) {
        if (noisyPlayers.containsKey(player)) return true;
        if (player.getVelocity().horizontalLengthSquared() > 0.0001) return true;
        if (!player.isOnGround() || player.fallDistance > 0.5f) return true;
        if (player.isUsingItem()) return true;
        if (player.handSwinging) return true;
        if (player.hurtTime > 0) return true;
        return false;
    }

    private static void checkShroomsNearPlayer(ServerWorld world, ServerPlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();
        int detectionRange = getDetectionRange(player);

        for (int dx = -detectionRange; dx <= detectionRange; dx++) {
            for (int dy = -detectionRange; dy <= detectionRange; dy++) {
                for (int dz = -detectionRange; dz <= detectionRange; dz++) {
                    BlockPos checkPos = playerPos.add(dx, dy, dz);
                    BlockState state = world.getBlockState(checkPos);

                    if (state.isOf(ModBlocks.BACTERIA_SHROOM_HORIZONTAL) ||
                            state.isOf(ModBlocks.BACTERIA_SHROOM_VERTICAL)) {

                        if (shroomCooldowns.containsKey(checkPos)) continue;

                        double distance = Math.sqrt(playerPos.getSquaredDistance(checkPos));

                        if (distance <= detectionRange) {
                            onShroomTriggered(world, checkPos, playerPos, player);
                            return;
                        }
                    }
                }
            }
        }
    }

    private static void onShroomTriggered(ServerWorld world, BlockPos shroomPos, BlockPos playerPos, ServerPlayerEntity player) {
        shroomCooldowns.put(shroomPos, COOLDOWN_TICKS);

        StrikeTracker tracker = strikeTrackers.get(shroomPos);
        if (tracker == null) {
            tracker = new StrikeTracker();
            strikeTrackers.put(shroomPos, tracker);
        }

        tracker.strikeCount++;
        tracker.firstStrikeTime = tracker.firstStrikeTime == 0 ? tickCounter : tracker.firstStrikeTime;

        // Track detections for advancement (any shroom, not just this specific one)
        UUID playerUuid = player.getUuid();
        int detections = playerShroomDetections.getOrDefault(playerUuid, 0) + 1;
        playerShroomDetections.put(playerUuid, detections);

        if (tracker.strikeCount >= STRIKES_TO_ALERT) {
            alertBacterium(world, shroomPos, playerPos, player);
            strikeTrackers.remove(shroomPos);

            world.playSound(null, shroomPos,
                    SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                    SoundCategory.HOSTILE, 0.8f, 1.5f);

            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.BLINDNESS, 100, 0, false, true));
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.DARKNESS, 100, 0, false, false));

            // Grant advancement when 3 detections reached
            if (detections >= 3) {

            }

        } else if (tracker.strikeCount == 2) {
            world.playSound(null, shroomPos,
                    SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS,
                    SoundCategory.HOSTILE, 0.6f, 1.8f);

            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.BLINDNESS, 40, 0, false, true));

        } else {
            world.playSound(null, shroomPos,
                    SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK,
                    SoundCategory.HOSTILE, 0.4f, 1.2f);

            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.DARKNESS, 20, 0, false, false));
        }
    }

    private static int getDetectionRange(ServerPlayerEntity player) {
        int baseRange = BASE_DETECTION_RANGE;
        boolean onQuietSurface = CarpetSoundReducer.isOnQuietSurface(player);
        boolean isSneaking = player.isSneaking();
        boolean isMovingFast = player.isSprinting() || !player.isOnGround();

        if (isSneaking) return (int)(baseRange * 0.3f);
        if (onQuietSurface && isMovingFast) return (int)(baseRange * 0.8f);
        if (onQuietSurface) return (int)(baseRange * 0.7f);
        return baseRange;
    }

    private static void alertBacterium(ServerWorld world, BlockPos shroomPos, BlockPos playerPos, ServerPlayerEntity player) {
        List<BacteriumEntity> bacteria = world.getEntitiesByClass(
                BacteriumEntity.class,
                new Box(playerPos).expand(200),
                bacterium -> true
        );

        if (!bacteria.isEmpty()) {
            BacteriumEntity bacterium = bacteria.get(0);
            bacterium.setSuspiciousLocation(playerPos);
            bacterium.setRunning(true);

            double dist = bacterium.getBlockPos().getSquaredDistance(playerPos);
            if (dist > 400) {
                java.util.Random random = new java.util.Random();
                BlockPos teleportPos = playerPos.add(random.nextInt(16) - 8, 0, random.nextInt(16) - 8);

                for (int y = 248; y >= 1; y--) {
                    BlockPos checkPos = new BlockPos(teleportPos.getX(), y, teleportPos.getZ());
                    if (world.getBlockState(checkPos.down()).isSolid() &&
                            world.getBlockState(checkPos).isAir() &&
                            world.getBlockState(checkPos.up()).isAir()) {

                        BacteriumAI.spawnEmergenceParticles(world, checkPos);

                        bacterium.refreshPositionAndAngles(
                                checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5,
                                bacterium.getYaw(), bacterium.getPitch());
                        break;
                    }
                }
            } else {
                BacteriumAI.spawnEmergenceParticles(world, bacterium.getBlockPos());
            }
        }
    }

    private static class StrikeTracker {
        int strikeCount = 0;
        int firstStrikeTime = 0;
    }
}