package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.entity.ModEntities;
import net.dalerd.backroomsbounded.entity.mimic.MimicEntity;
import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;

import java.util.List;
import java.util.Random;

public class MimicSpawnHandler {

    private static final Random RANDOM = new Random();
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 600; // Check every 30 seconds
    private static int ticksSinceLastMimic = 0;
    private static final int MIN_TIME_BETWEEN_MIMICS = 3600; // Minimum 3 minutes between spawns
    private static final int MAX_TIME_WITHOUT_MIMIC = 9600; // Force spawn if no mimic for 8 minutes

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            ticksSinceLastMimic++;

            if (tickCounter % CHECK_INTERVAL != 0) return;

            for (ServerWorld world : server.getWorlds()) {
                if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) continue;

                List<ServerPlayerEntity> players = world.getPlayers();
                if (players.isEmpty()) continue;

                int playerCount = players.size();

                // Count existing mimics
                long mimicCount = world.getEntitiesByType(ModEntities.MIMIC, m -> true).size();

                // Max mimics scales with player count
                int maxMimics = Math.min(playerCount, 3);
                if (mimicCount >= maxMimics) continue;

                // Don't spawn if one spawned recently (within 3 minutes)
                if (ticksSinceLastMimic < MIN_TIME_BETWEEN_MIMICS) continue;

                // Spawn chance: 20% base, +5% per player, +15% if no mimic for 8+ minutes
                float spawnChance = 0.20f + (playerCount - 1) * 0.05f;

                // Force spawn if no mimic for 8+ minutes
                if (ticksSinceLastMimic >= MAX_TIME_WITHOUT_MIMIC) {
                    spawnChance = 1.0f;
                }

                if (RANDOM.nextFloat() < spawnChance) {
                    ServerPlayerEntity targetPlayer = players.get(RANDOM.nextInt(players.size()));
                    spawnMimicNearPlayer(world, targetPlayer);
                    ticksSinceLastMimic = 0;
                }
            }
        });
    }

    private static void spawnMimicNearPlayer(ServerWorld world, ServerPlayerEntity player) {
        ChunkRandom random = new ChunkRandom(new CheckedRandom(world.getSeed() + tickCounter));

        for (int attempt = 0; attempt < 15; attempt++) {
            int chunkDist = 2 + RANDOM.nextInt(4); // 2-5 chunks away (closer)
            int offsetX = (RANDOM.nextBoolean() ? 1 : -1) * chunkDist * 16 + RANDOM.nextInt(16);
            int offsetZ = (RANDOM.nextBoolean() ? 1 : -1) * chunkDist * 16 + RANDOM.nextInt(16);

            BlockPos playerPos = player.getBlockPos();
            BlockPos spawnPos = new BlockPos(
                    playerPos.getX() + offsetX,
                    playerPos.getY(),
                    playerPos.getZ() + offsetZ
            );

            for (int y = 248; y >= 1; y--) {
                BlockPos checkPos = new BlockPos(spawnPos.getX(), y, spawnPos.getZ());
                if (world.getBlockState(checkPos.down()).isSolid() &&
                        world.getBlockState(checkPos).isAir() &&
                        world.getBlockState(checkPos.up()).isAir()) {

                    MimicEntity mimic = ModEntities.MIMIC.create(world);
                    if (mimic != null) {
                        List<ServerPlayerEntity> allPlayers = world.getPlayers();
                        ServerPlayerEntity playerToCopy = allPlayers.get(RANDOM.nextInt(allPlayers.size()));
                        mimic.initialize(playerToCopy);
                        mimic.refreshPositionAndAngles(
                                checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5, 0, 0);
                        world.spawnEntity(mimic);
                        return;
                    }
                }
            }
        }
    }
}
