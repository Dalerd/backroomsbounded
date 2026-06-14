package net.dalerd.backroomsbounded.sanity;

import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SanityDesktopFiles {

    private static final Map<UUID, Integer> fileCooldowns = new HashMap<>();
    private static final int FILE_COOLDOWN = 6000; // 5 minutes
    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            for (ServerWorld world : server.getWorlds()) {
                if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) continue;

                for (ServerPlayerEntity player : world.getPlayers()) {
                    int panic = SanityManager.getPanic(player);

                    // Only at terrified level (91+)
                    if (panic >= SanityManager.PANICKED) {
                        tryCreateDesktopFile(player, panic);
                    }
                }
            }
        });
    }

    private static void tryCreateDesktopFile(ServerPlayerEntity player, int panic) {
        UUID playerId = player.getUuid();
        int cooldown = fileCooldowns.getOrDefault(playerId, 0);

        if (cooldown > 0) {
            fileCooldowns.put(playerId, cooldown - 1);
            return;
        }

        ChunkRandom random = new ChunkRandom(new CheckedRandom(
                playerId.hashCode() + tickCounter
        ));

        // 5% chance per 5 minutes
        if (random.nextFloat() < 0.05f) {
            String desktopPath = System.getProperty("user.home") + "/Desktop/";
            createCreepyFile(desktopPath, player, random);
        }

        fileCooldowns.put(playerId, FILE_COOLDOWN);
    }

    private static void createCreepyFile(String path, ServerPlayerEntity player, ChunkRandom random) {
        int fileType = random.nextInt(3);
        String fileName;
        String content;

        switch (fileType) {
            case 0:
                fileName = "help_me.txt";
                content = "i can't find the exit\n"
                        + "the lights are flickering\n"
                        + "i hear something moving\n"
                        + "please help me\n"
                        + "- " + player.getName().getString();
                break;
            case 1:
                fileName = "they_know.json";
                content = "{\n"
                        + "  \"status\": \"watched\",\n"
                        + "  \"entity\": \"unknown\",\n"
                        + "  \"location\": \"level 0\",\n"
                        + "  \"survival_time\": \"" + (random.nextInt(48) + 1) + " hours\",\n"
                        + "  \"message\": \"it knows you're here\"\n"
                        + "}";
                break;
            case 2:
            default:
                fileName = "run.txt";
                content = "DON'T STOP MOVING\n"
                        + "IT CAN HEAR YOU BREATHING\n"
                        + "THE EXIT IS A LIE\n"
                        + "TRUST NO ONE";
                break;
        }

        try {
            File file = new File(path + fileName);

            // Don't overwrite existing files
            if (file.exists()) {
                fileName = fileName.replace(".", "_" + random.nextInt(100) + ".");
                file = new File(path + fileName);
            }

            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            // Silently fail - don't crash the game
        }
    }
}
