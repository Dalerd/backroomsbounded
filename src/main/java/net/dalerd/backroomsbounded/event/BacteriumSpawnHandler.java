package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.entity.ModEntities;
import net.dalerd.backroomsbounded.entity.bacterium.BacteriumEntity;
import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.*;

public class BacteriumSpawnHandler {

    private static final Random RANDOM = new Random();
    private static int tickCounter = 0;
    private static int respawnCooldown = 0;
    private static final int CHECK_INTERVAL = 100;
    private static final int RESPAWN_DELAY = 300;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            if (tickCounter % CHECK_INTERVAL != 0) return;

            for (ServerWorld world : server.getWorlds()) {
                if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) continue;

                List<ServerPlayerEntity> players = world.getPlayers();
                if (players.isEmpty()) continue;

                // Count bacteria - MUST be exactly 1
                long bacteriumCount = world.getEntitiesByType(ModEntities.BACTERIUM, e -> true).size();

                if (bacteriumCount >= 1) {
                    respawnCooldown = 0;
                    continue;
                }

                // Only spawn if count is 0
                respawnCooldown += CHECK_INTERVAL;
                if (respawnCooldown < RESPAWN_DELAY) continue;

                respawnCooldown = 0;
                ServerPlayerEntity targetPlayer = players.get(RANDOM.nextInt(players.size()));
                spawnBacteriumNearPlayer(world, targetPlayer);
            }
        });
    }

    private static void spawnBacteriumNearPlayer(ServerWorld world, ServerPlayerEntity player) {
        // Double-check no bacterium exists before spawning
        if (world.getEntitiesByType(ModEntities.BACTERIUM, e -> true).size() >= 1) {
            return;
        }

        for (int attempt = 0; attempt < 20; attempt++) {
            int chunkDist = 5 + RANDOM.nextInt(4);
            int offsetX = (RANDOM.nextBoolean() ? 1 : -1) * chunkDist * 16 + RANDOM.nextInt(16);
            int offsetZ = (RANDOM.nextBoolean() ? 1 : -1) * chunkDist * 16 + RANDOM.nextInt(16);

            BlockPos playerPos = player.getBlockPos();
            BlockPos spawnPos = new BlockPos(
                    playerPos.getX() + offsetX,
                    playerPos.getY(),
                    playerPos.getZ() + offsetZ
            );

            BlockPos safePos = findSafePosition(world, spawnPos);
            if (safePos != null) {
                BacteriumEntity bacterium = ModEntities.BACTERIUM.create(world);
                if (bacterium != null) {
                    bacterium.refreshPositionAndAngles(
                            safePos.getX() + 0.5,
                            safePos.getY(),
                            safePos.getZ() + 0.5,
                            0, 0
                    );
                    world.spawnEntity(bacterium);
                    return;
                }
            }
        }

        // Fallback
        BlockPos playerPos = player.getBlockPos();
        BlockPos fallbackPos = new BlockPos(playerPos.getX() + 128, playerPos.getY(), playerPos.getZ() + 128);
        BlockPos safePos = findSafePosition(world, fallbackPos);
        if (safePos != null) {
            // Final double-check
            if (world.getEntitiesByType(ModEntities.BACTERIUM, e -> true).size() >= 1) return;

            BacteriumEntity bacterium = ModEntities.BACTERIUM.create(world);
            if (bacterium != null) {
                bacterium.refreshPositionAndAngles(
                        safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, 0, 0
                );
                world.spawnEntity(bacterium);
            }
        }
    }

    private static BlockPos findSafePosition(ServerWorld world, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL);

        int playerY = pos.getY();
        int floorBase = (playerY / 8) * 8;

        int[] floorsToCheck = { floorBase, floorBase + 8, floorBase - 8 };

        for (int floorY : floorsToCheck) {
            if (floorY < 8 || floorY > 248) continue;
            int walkY = floorY + 1;
            if (walkY >= 8 && walkY <= 248) {
                BlockPos checkPos = new BlockPos(pos.getX(), walkY, pos.getZ());
                BlockState below = world.getBlockState(checkPos.down());
                BlockState at = world.getBlockState(checkPos);
                BlockState above = world.getBlockState(checkPos.up());
                if (below.isSolid() && at.isAir() && above.isAir()) return checkPos;
            }
        }

        for (int y = 248; y >= 1; y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            BlockState below = world.getBlockState(checkPos.down());
            BlockState at = world.getBlockState(checkPos);
            BlockState above = world.getBlockState(checkPos.up());
            if (below.isSolid() && at.isAir() && above.isAir()) return checkPos;
        }
        return null;
    }
}