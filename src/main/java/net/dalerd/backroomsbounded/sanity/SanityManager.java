package net.dalerd.backroomsbounded.sanity;

import net.minecraft.server.network.ServerPlayerEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SanityManager {

    // Panic levels per player (0-100)
    private static final Map<UUID, Integer> panicLevels = new HashMap<>();

    // Time in darkness per player (ticks)
    private static final Map<UUID, Integer> darknessTicks = new HashMap<>();

    // Constants
    public static final int MAX_PANIC = 100;
    public static final int MIN_PANIC = 0;
    public static final int DARKNESS_PANIC_THRESHOLD = 400; // 20 seconds of darkness to start panic

    // Panic thresholds for effects
    public static final int CALM = 30;
    public static final int UNEASY = 50;
    public static final int ANXIOUS = 70;
    public static final int PANICKED = 90;
    public static final int TERRIFIED = 100;

    public static int getPanic(ServerPlayerEntity player) {
        return panicLevels.getOrDefault(player.getUuid(), 0);
    }

    public static void setPanic(ServerPlayerEntity player, int level) {
        panicLevels.put(player.getUuid(), Math.clamp(level, MIN_PANIC, MAX_PANIC));
    }

    public static void addPanic(ServerPlayerEntity player, int amount) {
        int current = getPanic(player);
        setPanic(player, current + amount);
    }

    public static void reducePanic(ServerPlayerEntity player, int amount) {
        int current = getPanic(player);
        setPanic(player, current - amount);
    }

    public static int getDarknessTicks(ServerPlayerEntity player) {
        return darknessTicks.getOrDefault(player.getUuid(), 0);
    }

    public static void setDarknessTicks(ServerPlayerEntity player, int ticks) {
        darknessTicks.put(player.getUuid(), Math.max(0, ticks));
    }

    public static void incrementDarknessTicks(ServerPlayerEntity player) {
        darknessTicks.put(player.getUuid(), getDarknessTicks(player) + 1);
    }

    public static void resetDarknessTicks(ServerPlayerEntity player) {
        darknessTicks.put(player.getUuid(), 0);
    }

    public static void onPlayerDeath(ServerPlayerEntity player) {
        panicLevels.put(player.getUuid(), 0);
        darknessTicks.put(player.getUuid(), 0);
    }

    public static void onPlayerLoggedOut(ServerPlayerEntity player) {
        panicLevels.remove(player.getUuid());
        darknessTicks.remove(player.getUuid());
    }
}