package net.dalerd.backroomsbounded.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;

public class PortalBlocker {

    public static void register() {

        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> {

                }
        );
    }

    public static boolean isInBackrooms(ServerPlayerEntity player) {

        return player.getWorld()
                .getRegistryKey()
                .getValue()
                .toString()
                .equals("backroomsbounded:backrooms");
    }
}
