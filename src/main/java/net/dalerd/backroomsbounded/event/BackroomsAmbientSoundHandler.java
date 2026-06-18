package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.sound.ModSounds;
import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;

import java.util.*;

public class BackroomsAmbientSoundHandler {

    private static int tickCounter = 0;
    private static final int AMBIENT_INTERVAL = 600; // 30 seconds
    private static final int MOOD_INTERVAL = 2400; // 2 minutes
    private static final Random RANDOM = new Random();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            for (ServerWorld world : server.getWorlds()) {
                if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) continue;

                for (ServerPlayerEntity player : world.getPlayers()) {
                    // Ambient loop every 30 seconds
                    if (tickCounter % AMBIENT_INTERVAL == 0) {
                        player.playSoundToPlayer(ModSounds.AMBIENT_LOOP,
                                SoundCategory.AMBIENT, 0.3f, 1.0f);
                    }

                    // Mood sounds every 2 minutes (random chance)
                    if (tickCounter % MOOD_INTERVAL == 0 && RANDOM.nextFloat() < 0.3f) {
                        player.playSoundToPlayer(ModSounds.AMBIENT_MOOD,
                                SoundCategory.AMBIENT, 0.4f, 0.8f + RANDOM.nextFloat() * 0.4f);
                    }
                }
            }
        });
    }
}