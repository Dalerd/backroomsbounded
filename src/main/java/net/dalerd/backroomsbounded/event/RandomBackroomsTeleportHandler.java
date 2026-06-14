package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class RandomBackroomsTeleportHandler {

    private static final Random RANDOM = new Random();
    private static final int SEARCH_RADIUS = 5000;
    private static final int MAX_ATTEMPTS = 100;

    // Players that should respawn in backrooms
    private static final Set<UUID> RESPAWN_TO_BACKROOMS = new HashSet<>();

    public static void register() {

        // =========================================
        // DEATH -> 40%
        // =========================================

        ServerLivingEntityEvents.AFTER_DEATH.register(
                (entity, damageSource) -> {

                    if (!(entity instanceof ServerPlayerEntity player)) {
                        return;
                    }

                    if (isInBackrooms(player)) {
                        return;
                    }

                    if (RANDOM.nextFloat() < 0.40f) {
                        RESPAWN_TO_BACKROOMS.add(player.getUuid());
                    }
                }
        );

        // =========================================
        // RESPAWN
        // =========================================

        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> {

                    // =====================================
                    // DIED INSIDE BACKROOMS
                    // Respawn back into backrooms
                    // =====================================

                    if (oldPlayer.getWorld().getRegistryKey()
                            == BackroomsDimension.BACKROOMS_LEVEL_KEY) {

                        teleportToBackrooms(newPlayer);
                        return;
                    }

                    // =====================================
                    // NORMAL DEATH TELEPORT CHANCE
                    // =====================================

                    if (!RESPAWN_TO_BACKROOMS.contains(newPlayer.getUuid())) {
                        return;
                    }

                    RESPAWN_TO_BACKROOMS.remove(newPlayer.getUuid());
                    teleportToBackrooms(newPlayer);
                }
        );

        // =========================================
        // DAMAGE -> 5%
        // =========================================

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (entity, source, amount) -> {

                    if (!(entity instanceof ServerPlayerEntity player)) {
                        return true;
                    }

                    if (isInBackrooms(player)) {
                        return true;
                    }

                    boolean validDamage =
                            source == player.getDamageSources().fall()
                                    ||
                                    source.getAttacker() != null;

                    if (!validDamage) {
                        return true;
                    }

                    if (RANDOM.nextFloat() < 0.05f) {
                        teleportToBackrooms(player);
                    }

                    return true;
                }
        );

        // =========================================
        // SLEEP -> 20%
        // =========================================

        EntitySleepEvents.STOP_SLEEPING.register(
                (entity, sleepingPos) -> {

                    if (!(entity instanceof ServerPlayerEntity player)) {
                        return;
                    }

                    if (isInBackrooms(player)) {
                        return;
                    }

                    if (RANDOM.nextFloat() < 0.20f) {
                        teleportToBackrooms(player);
                    }
                }
        );
    }

    // =========================================
    // PORTAL CHANCE
    // =========================================

    public static void tryPortalTeleport(ServerPlayerEntity player) {
        if (isInBackrooms(player)) {
            return;
        }

        // 10%
        if (RANDOM.nextFloat() < 0.10f) {
            teleportToBackrooms(player);
        }
    }

    // =========================================
    // TELEPORT TO BACKROOMS
    // =========================================

    private static void teleportToBackrooms(ServerPlayerEntity player) {
        ServerWorld backroomsWorld = player.getServer()
                .getWorld(BackroomsDimension.BACKROOMS_LEVEL_KEY);

        if (backroomsWorld == null) {
            return;
        }

        // Try to find a safe random position
        BlockPos safePos = findSafeTeleportPosition(backroomsWorld);

        if (safePos != null) {
            player.teleport(
                    backroomsWorld,
                    safePos.getX() + 0.5,
                    safePos.getY(),
                    safePos.getZ() + 0.5,
                    player.getYaw(),
                    player.getPitch()
            );
        } else {
            // Fallback to default position
            player.teleport(
                    backroomsWorld,
                    0.5,
                    8,
                    0.5,
                    player.getYaw(),
                    player.getPitch()
            );
        }
    }

    // =========================================
    // FIND SAFE TELEPORT POSITION
    // =========================================

    private static BlockPos findSafeTeleportPosition(ServerWorld world) {
        long worldSeed = world.getSeed();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // Generate random coordinates
            ChunkRandom random = new ChunkRandom(new CheckedRandom(worldSeed + attempt));
            int x = (random.nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS) * 16;
            int z = (random.nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS) * 16;

            // Try to find a safe Y level at these coordinates
            int y = findSafeYLevel(world, x, z);

            if (y > 0 && isValidSpawnPosition(world, x, y, z)) {
                return new BlockPos(x, y, z);
            }
        }

        return null; // No safe position found
    }

    // =========================================
    // FIND SAFE Y LEVEL
    // =========================================

    private static int findSafeYLevel(ServerWorld world, int x, int z) {
        // Load the chunk if needed
        ChunkPos chunkPos = new ChunkPos(x >> 4, z >> 4);
        world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL);

        // Check Y levels from 8 to 248 (middle floors only, avoid bottom and top)
        for (int y = 248; y >= 8; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState blockBelow = world.getBlockState(pos.down());
            BlockState blockAt = world.getBlockState(pos);
            BlockState blockAbove = world.getBlockState(pos.up());
            BlockState blockAbove2 = world.getBlockState(pos.up(2));

            // Check: solid floor below, 3 blocks of air above
            if (blockBelow.isSolid() &&
                    blockAt.isAir() &&
                    blockAbove.isAir() &&
                    blockAbove2.isAir()) {
                return y;
            }
        }
        return -1; // No safe Y level found
    }

    // =========================================
    // VALIDATE SPAWN POSITION
    // =========================================

    private static boolean isValidSpawnPosition(ServerWorld world, int x, int y, int z) {
        // Check all 4 cardinal directions for at least 1 block of air
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos sidePos = new BlockPos(x, y, z).offset(direction);
            if (!world.getBlockState(sidePos).isAir()) {
                return false;
            }
        }

        // Check that floor is a proper solid block
        BlockPos floorPos = new BlockPos(x, y - 1, z);
        BlockState floorBlock = world.getBlockState(floorPos);

        // Accept any solid block as floor (yellow terracotta, backboard, etc.)
        return floorBlock.isSolid();
    }

    // =========================================
    // CHECK DIMENSION
    // =========================================

    private static boolean isInBackrooms(ServerPlayerEntity player) {
        return player.getWorld()
                .getRegistryKey()
                == BackroomsDimension.BACKROOMS_LEVEL_KEY;
    }
}
