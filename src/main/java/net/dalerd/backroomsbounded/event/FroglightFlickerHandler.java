package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;

import java.util.*;

public class FroglightFlickerHandler {

    private static final Map<BlockPos, Integer> flickeringLights = new HashMap<>();
    private static final Map<BlockPos, Boolean> lightStates = new HashMap<>(); // true = froglight, false = bone
    private static int tickCounter = 0;

    // Flicker settings
    private static final int FLICKER_CHECK_INTERVAL = 10; // Check every 10 ticks (0.5 seconds)
    private static final double FLICKER_CHANCE = 0.15; // 15% chance to flicker when checked
    private static final int MAX_FLICKER_DURATION = 40; // Max 2 seconds of darkness
    private static final int SCAN_RADIUS = 64; // Scan radius around each player

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            if (tickCounter % FLICKER_CHECK_INTERVAL != 0) return;

            for (ServerWorld world : server.getWorlds()) {
                if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) continue;

                // Process flickering lights
                processFlickeringLights(world);

                // Scan for new lights to flicker
                if (tickCounter % 40 == 0) { // Every 2 seconds
                    scanForLights(world);
                }
            }
        });
    }

    private static void processFlickeringLights(ServerWorld world) {
        Iterator<Map.Entry<BlockPos, Integer>> iterator = flickeringLights.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            BlockPos pos = entry.getKey();
            int remainingTicks = entry.getValue() - FLICKER_CHECK_INTERVAL;

            if (remainingTicks <= 0) {
                // Restore original state
                restoreLight(world, pos);
                iterator.remove();
                lightStates.remove(pos);
            } else {
                // Toggle light state for flicker effect
                toggleLight(world, pos);
                entry.setValue(remainingTicks);
            }
        }
    }

    private static void toggleLight(ServerWorld world, BlockPos pos) {
        Boolean isFroglight = lightStates.get(pos);
        if (isFroglight == null) return;

        if (isFroglight) {
            // Turn off (replace with bone block)
            world.setBlockState(pos, Blocks.BONE_BLOCK.getDefaultState());
            lightStates.put(pos, false);
        } else {
            // Turn on (replace with froglight)
            world.setBlockState(pos, Blocks.OCHRE_FROGLIGHT.getDefaultState());
            lightStates.put(pos, true);
        }
    }

    private static void restoreLight(ServerWorld world, BlockPos pos) {
        // Always restore to froglight when done flickering
        world.setBlockState(pos, Blocks.OCHRE_FROGLIGHT.getDefaultState());
    }

    private static void scanForLights(ServerWorld world) {
        ChunkRandom random = new ChunkRandom(new CheckedRandom(world.getSeed() + tickCounter));

        world.getPlayers().forEach(player -> {
            BlockPos playerPos = player.getBlockPos();

            // Scan around player
            for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx += 4) {
                for (int dy = -8; dy <= 8; dy += 4) {
                    for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz += 4) {
                        BlockPos pos = playerPos.add(dx, dy, dz);

                        // Skip if already flickering
                        if (flickeringLights.containsKey(pos)) continue;

                        // Check if it's a froglight
                        if (world.getBlockState(pos).isOf(Blocks.OCHRE_FROGLIGHT)) {
                            // Random chance to start flickering
                            if (random.nextFloat() < FLICKER_CHANCE) {
                                int duration = 10 + random.nextInt(MAX_FLICKER_DURATION);
                                flickeringLights.put(pos, duration);
                                lightStates.put(pos, true);
                            }
                        }
                    }
                }
            }
        });
    }
}
