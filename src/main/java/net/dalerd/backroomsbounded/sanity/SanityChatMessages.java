package net.dalerd.backroomsbounded.sanity;

import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;

import java.util.*;

public class SanityChatMessages {

    private static final Map<UUID, Integer> messageCooldowns = new HashMap<>();
    private static final int MESSAGE_MIN_COOLDOWN = 2400; // 2 minutes
    private static final int MESSAGE_MAX_COOLDOWN = 6000; // 5 minutes

    private static int tickCounter = 0;

    // Creepy messages that appear to be from the player
    private static final String[] PLAYER_MESSAGES = {
            "Is someone there?",
            "I can hear breathing",
            "The lights are flickering",
            "I think I saw something move",
            "This place never ends",
            "I want to go home",
            "Hello?",
            "My head hurts",
            "The walls are closing in",
            "I've been here before"
    };

    // Messages that appear to be from an unknown entity
    private static final String[] UNKNOWN_MESSAGES = {
            "Don't turn around",
            "It's behind you",
            "You're not alone",
            "Keep walking",
            "They can hear you",
            "The exit is a lie",
            "Stop making noise",
            "It knows your name",
            "You can't escape",
            "Look up"
    };

    // Fake join/leave messages
    private static final String[] JOIN_MESSAGES = {
            "joined the game",
            "connected",
            "entered the backrooms"
    };

    private static final String[] LEAVE_MESSAGES = {
            "left the game",
            "disconnected",
            "was lost"
    };

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            for (ServerWorld world : server.getWorlds()) {
                if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) continue;

                for (ServerPlayerEntity player : world.getPlayers()) {
                    int panic = SanityManager.getPanic(player);

                    // Only send fake messages at panic 70+
                    if (panic >= SanityManager.ANXIOUS) {
                        trySendFakeMessage(player, panic);
                    }
                }
            }
        });
    }

    private static void trySendFakeMessage(ServerPlayerEntity player, int panic) {
        UUID playerId = player.getUuid();
        int cooldown = messageCooldowns.getOrDefault(playerId, 0);

        if (cooldown > 0) {
            messageCooldowns.put(playerId, cooldown - 1);
            return;
        }

        ChunkRandom random = new ChunkRandom(new CheckedRandom(
                playerId.hashCode() + tickCounter
        ));

        // Higher panic = more frequent messages
        float chance = 0.02f + ((panic - 70) * 0.005f);

        if (random.nextFloat() < chance) {
            float messageType = random.nextFloat();

            if (messageType < 0.35f) {
                // Player message (35%)
                String msg = PLAYER_MESSAGES[random.nextInt(PLAYER_MESSAGES.length)];
                player.sendMessage(Text.literal("<" + player.getName().getString() + "> " + msg)
                        .formatted(Formatting.GRAY), false);
            } else if (messageType < 0.65f) {
                // Unknown message (30%)
                String msg = UNKNOWN_MESSAGES[random.nextInt(UNKNOWN_MESSAGES.length)];
                player.sendMessage(Text.literal("<???> " + msg)
                        .formatted(Formatting.DARK_GRAY).formatted(Formatting.ITALIC), false);
            } else if (messageType < 0.80f) {
                // Fake join (15%)
                String fakeName = getRandomFakeName(player, random);
                String joinMsg = JOIN_MESSAGES[random.nextInt(JOIN_MESSAGES.length)];
                player.sendMessage(Text.literal(fakeName + " " + joinMsg)
                        .formatted(Formatting.YELLOW), false);
            } else {
                // Fake leave (20%)
                String fakeName = getRandomFakeName(player, random);
                String leaveMsg = LEAVE_MESSAGES[random.nextInt(LEAVE_MESSAGES.length)];
                player.sendMessage(Text.literal(fakeName + " " + leaveMsg)
                        .formatted(Formatting.YELLOW), false);
            }
        }

        int newCooldown = MESSAGE_MIN_COOLDOWN + random.nextInt(MESSAGE_MAX_COOLDOWN - MESSAGE_MIN_COOLDOWN);
        messageCooldowns.put(playerId, newCooldown);
    }

    private static String getRandomFakeName(ServerPlayerEntity player, ChunkRandom random) {
        // Get other players on the server (if any)
        List<String> otherPlayerNames = new ArrayList<>();
        for (ServerPlayerEntity other : player.getServer().getPlayerManager().getPlayerList()) {
            if (other != player) {
                otherPlayerNames.add(other.getName().getString());
            }
        }

        // 50% chance to use a real player name if available
        if (!otherPlayerNames.isEmpty() && random.nextFloat() < 0.5f) {
            return otherPlayerNames.get(random.nextInt(otherPlayerNames.size()));
        }

        // Otherwise use a random creepy name
        String[] fakeNames = {
                "Herobrine",
                "Entity_303",
                "Null",
                "LostSoul",
                "TheWatcher",
                "Ghost",
                "Shadow",
                "Unknown",
                "FadedMemory",
                "Echo",
                "AlbertGovyadina",
                "Steve"
        };
        return fakeNames[random.nextInt(fakeNames.length)];
    }
}
