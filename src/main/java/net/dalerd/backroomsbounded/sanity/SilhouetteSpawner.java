package net.dalerd.backroomsbounded.sanity;

import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;

import java.util.*;

public class SilhouetteSpawner {

    private static final Map<UUID, Integer> spawnCooldowns = new HashMap<>();
    private static final int SPAWN_MIN_COOLDOWN = 300;
    private static final int SPAWN_MAX_COOLDOWN = 600;
    private static final int MAX_SILHOUETTES = 3;
    private static final int MIN_DISTANCE = 16;
    private static final int MAX_DISTANCE = 48;

    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            for (ServerWorld world : server.getWorlds()) {
                if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) continue;

                for (ServerPlayerEntity player : world.getPlayers()) {
                    int panic = SanityManager.getPanic(player);

                    if (panic >= SanityManager.ANXIOUS) {
                        trySpawnSilhouette(player, panic);
                    }
                }
            }
        });
    }

    private static void trySpawnSilhouette(ServerPlayerEntity player, int panic) {
        UUID playerId = player.getUuid();
        int cooldown = spawnCooldowns.getOrDefault(playerId, 0);

        if (cooldown > 0) {
            spawnCooldowns.put(playerId, cooldown - 1);
            return;
        }

        // Count nearby silhouettes
        long nearbyCount = player.getServerWorld().getEntitiesByType(
                EntityType.ENDERMAN,
                entity -> entity.squaredDistanceTo(player) < MAX_DISTANCE * MAX_DISTANCE
                        && entity.isInvisible()
        ).size();

        if (nearbyCount >= MAX_SILHOUETTES) return;

        ChunkRandom random = new ChunkRandom(new CheckedRandom(
                playerId.hashCode() + tickCounter
        ));

        float chance = 0.03f + ((panic - 70) * 0.01f);

        if (random.nextFloat() < chance) {
            BlockPos spawnPos = findSpawnPosition(player, random);

            if (spawnPos != null) {
                spawnDarkSilhouette(player, spawnPos, random);
            }
        }

        int newCooldown = SPAWN_MIN_COOLDOWN + random.nextInt(SPAWN_MAX_COOLDOWN - SPAWN_MIN_COOLDOWN);
        spawnCooldowns.put(playerId, newCooldown);
    }

    private static void spawnDarkSilhouette(ServerPlayerEntity player, BlockPos pos, ChunkRandom random) {
        ServerWorld world = player.getServerWorld();

        // 70% chance for Enderman silhouette, 30% for Armor Stand (player-shaped)
        if (random.nextFloat() < 0.7f) {
            EndermanEntity enderman = EntityType.ENDERMAN.create(world);
            if (enderman != null) {
                enderman.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
                enderman.setInvisible(true);
                enderman.setSilent(true);
                enderman.setNoGravity(true);
                enderman.setInvulnerable(true);
                enderman.setCustomNameVisible(false);
                enderman.setAiDisabled(true);
                world.spawnEntity(enderman);

                // Schedule removal after 5 seconds
                scheduleRemoval(world, enderman, 100);
            }
        } else {
            ArmorStandEntity armorStand = EntityType.ARMOR_STAND.create(world);
            if (armorStand != null) {
                armorStand.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
                armorStand.setInvisible(true);
                armorStand.setSilent(true);
                armorStand.setNoGravity(true);
                armorStand.setInvulnerable(true);
                armorStand.setCustomNameVisible(false);
                // Put a black block on head for shadow appearance
                armorStand.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.BLACK_CONCRETE));
                world.spawnEntity(armorStand);

                scheduleRemoval(world, armorStand, 100);
            }
        }
    }

    private static void scheduleRemoval(ServerWorld world, net.minecraft.entity.Entity entity, int delayTicks) {
        // Use server tick to remove after delay
        world.getServer().execute(() -> {
            try {
                Thread.sleep(delayTicks * 50);
            } catch (InterruptedException e) {}
            if (entity.isAlive()) {
                entity.remove(net.minecraft.entity.Entity.RemovalReason.DISCARDED);
            }
        });
    }

    private static BlockPos findSpawnPosition(ServerPlayerEntity player, ChunkRandom random) {
        for (int attempt = 0; attempt < 10; attempt++) {
            Direction dir = Direction.fromHorizontal(random.nextInt(4));
            int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE);

            BlockPos pos = player.getBlockPos()
                    .offset(dir, distance)
                    .add(random.nextInt(8) - 4, 0, random.nextInt(8) - 4);

            if (player.getServerWorld().getBlockState(pos.down()).isSolid() &&
                    player.getServerWorld().getBlockState(pos).isAir() &&
                    player.getServerWorld().getBlockState(pos.up()).isAir()) {
                return pos;
            }
        }
        return null;
    }
}