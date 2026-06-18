package net.dalerd.backroomsbounded.entity.mimic;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;

public class MimicSkinManager {

    private static final Map<UUID, UUID> playerMimicMap = new HashMap<>();
    private static final String[] FALLBACK_NAMES = {
            "Steve", "Alex", "Herobrine", "Entity303", "Null"
    };

    public static void assignSkin(MimicEntity mimic, List<ServerPlayerEntity> players) {
        if (!players.isEmpty()) {
            // Copy a real player
            PlayerEntity randomPlayer = players.get(new Random().nextInt(players.size()));
            mimic.setCopiedPlayerName(randomPlayer.getName().getString());
            mimic.setCopiedPlayerUUID(randomPlayer.getUuid());
        } else {
            // No players available - use a creepy fallback name
            String fallbackName = FALLBACK_NAMES[new Random().nextInt(FALLBACK_NAMES.length)];
            mimic.setCopiedPlayerName(fallbackName);
            mimic.setCopiedPlayerUUID(UUID.randomUUID());
        }
        playerMimicMap.put(mimic.getUuid(), mimic.getCopiedPlayerUUID());
    }

    public static UUID getCopiedPlayerUUID(MimicEntity mimic) {
        return playerMimicMap.get(mimic.getUuid());
    }
}