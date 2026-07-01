package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.advancement.AdvancementManager;
import net.dalerd.backroomsbounded.advancement.ModAdvancements;
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

import java.util.*;

public class RandomBackroomsTeleportHandler {

    private static final Random RANDOM = new Random();
    private static final int SEARCH_RADIUS = 5000;
    private static final int MAX_ATTEMPTS = 100;

    private static final Set<UUID> RESPAWN_TO_BACKROOMS = new HashSet<>();

    public static void register() {

        // =========================================
        // DEATH -> 40%
        // =========================================
        ServerLivingEntityEvents.AFTER_DEATH.register(
                (entity, damageSource) -> {
                    if (!(entity instanceof ServerPlayerEntity player)) return;
                    if (isInBackrooms(player)) return;
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
                    if (oldPlayer.getWorld().getRegistryKey() == BackroomsDimension.BACKROOMS_LEVEL_KEY) {
                        teleportToBackrooms(newPlayer);
                        return;
                    }
                    if (!RESPAWN_TO_BACKROOMS.contains(newPlayer.getUuid())) return;
                    RESPAWN_TO_BACKROOMS.remove(newPlayer.getUuid());
                    teleportToBackrooms(newPlayer);
                }
        );

        // =========================================
        // DAMAGE -> 5%
        // =========================================
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (entity, source, amount) -> {
                    if (!(entity instanceof ServerPlayerEntity player)) return true;
                    if (isInBackrooms(player)) return true;
                    boolean validDamage = source == player.getDamageSources().fall() || source.getAttacker() != null;
                    if (!validDamage) return true;
                    if (RANDOM.nextFloat() < 0.05f) teleportToBackrooms(player);
                    return true;
                }
        );

        // =========================================
        // SLEEP -> 20%
        // =========================================
        EntitySleepEvents.STOP_SLEEPING.register(
                (entity, sleepingPos) -> {
                    if (!(entity instanceof ServerPlayerEntity player)) return;
                    if (isInBackrooms(player)) return;
                    if (RANDOM.nextFloat() < 0.20f) teleportToBackrooms(player);
                }
        );
    }

    // =========================================
    // PORTAL CHANCE
    // =========================================
    public static void tryPortalTeleport(ServerPlayerEntity player) {
        if (isInBackrooms(player)) return;
        if (RANDOM.nextFloat() < 0.10f) teleportToBackrooms(player);
    }

    // =========================================
    // TELEPORT TO BACKROOMS
    // =========================================
    private static void teleportToBackrooms(ServerPlayerEntity player) {
        ServerWorld backroomsWorld = player.getServer().getWorld(BackroomsDimension.BACKROOMS_LEVEL_KEY);
        if (backroomsWorld == null) return;

        BlockPos safePos = findSafeTeleportPosition(backroomsWorld);
        if (safePos != null) {
            player.teleport(backroomsWorld, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5,
                    player.getYaw(), player.getPitch());
            AdvancementManager.enterBackrooms(player);
        } else {
            player.teleport(backroomsWorld, 0.5, 8, 0.5, player.getYaw(), player.getPitch());
            AdvancementManager.enterBackrooms(player);
        }

    }

    // =========================================
    // FIND SAFE TELEPORT POSITION (RANDOM EACH TIME)
    // =========================================
    private static BlockPos findSafeTeleportPosition(ServerWorld world) {
        // Mix world seed with current time and random for unique positions every time
        long seed = world.getSeed() ^ System.currentTimeMillis() ^ RANDOM.nextLong();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            ChunkRandom random = new ChunkRandom(new CheckedRandom(seed + attempt * 7919L));
            int x = (random.nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS) * 16;
            int z = (random.nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS) * 16;
            int y = findSafeYLevel(world, x, z);
            if (y > 0 && isValidSpawnPosition(world, x, y, z)) return new BlockPos(x, y, z);
        }
        return null;
    }

    private static int findSafeYLevel(ServerWorld world, int x, int z) {
        ChunkPos chunkPos = new ChunkPos(x >> 4, z >> 4);
        world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL);
        for (int y = 248; y >= 1; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (world.getBlockState(pos.down()).isSolid() &&
                    world.getBlockState(pos).isAir() &&
                    world.getBlockState(pos.up()).isAir() &&
                    world.getBlockState(pos.up(2)).isAir()) return y;
        }
        return -1;
    }

    private static boolean isValidSpawnPosition(ServerWorld world, int x, int y, int z) {
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (!world.getBlockState(new BlockPos(x, y, z).offset(direction)).isAir()) return false;
        }
        return world.getBlockState(new BlockPos(x, y - 1, z)).isSolid();
    }

    private static boolean isInBackrooms(ServerPlayerEntity player) {
        return player.getWorld().getRegistryKey() == BackroomsDimension.BACKROOMS_LEVEL_KEY;
    }
}
