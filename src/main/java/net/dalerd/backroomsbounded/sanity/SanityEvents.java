package net.dalerd.backroomsbounded.sanity;

import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.LightType;

public class SanityEvents {

    private static int tickCounter = 0;
    private static final int PANIC_CHECK_INTERVAL = 100; // Every 5 seconds

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            if (tickCounter % PANIC_CHECK_INTERVAL != 0) return;

            for (ServerWorld world : server.getWorlds()) {
                // Only process backrooms dimension
                if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) continue;

                for (ServerPlayerEntity player : world.getPlayers()) {
                    updatePanic(player);
                }
            }
        });
    }

    private static void updatePanic(ServerPlayerEntity player) {
        int lightLevel = player.getWorld().getLightLevel(LightType.BLOCK, player.getBlockPos());

        if (lightLevel <= 4) {
            // In darkness - increase panic
            SanityManager.incrementDarknessTicks(player);

            if (SanityManager.getDarknessTicks(player) >= SanityManager.DARKNESS_PANIC_THRESHOLD) {
                if (lightLevel == 0) {
                    SanityManager.addPanic(player, 2); // Deep darkness = faster panic
                } else {
                    SanityManager.addPanic(player, 1); // Low light = slow panic
                }
            }
        } else if (lightLevel >= 8) {
            // In light - decrease panic
            SanityManager.resetDarknessTicks(player);
            SanityManager.reducePanic(player, 1);
        }

        // Clamp values
        int panic = SanityManager.getPanic(player);
        if (panic < SanityManager.MIN_PANIC) SanityManager.setPanic(player, SanityManager.MIN_PANIC);
        if (panic > SanityManager.MAX_PANIC) SanityManager.setPanic(player, SanityManager.MAX_PANIC);
    }
}
