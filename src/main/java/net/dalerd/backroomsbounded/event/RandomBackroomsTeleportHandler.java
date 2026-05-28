package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class RandomBackroomsTeleportHandler {

    private static final Random RANDOM = new Random();

    // Players that should respawn in backrooms
    private static final Set<UUID> RESPAWN_TO_BACKROOMS =
            new HashSet<>();

    public static void register() {

        // =========================================
        // DEATH -> 40%
        // =========================================

        ServerLivingEntityEvents.AFTER_DEATH.register(
                (entity, damageSource) -> {

                    if (!(entity instanceof ServerPlayerEntity player)) {
                        return;
                    }

                    if (isInBackrooms(player)) {
                        return;
                    }

                    if (RANDOM.nextFloat() < 0.40f) {

                        RESPAWN_TO_BACKROOMS.add(
                                player.getUuid()
                        );
                    }
                }
        );

// =========================================
// RESPAWN
// =========================================

        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> {

                    // =====================================
                    // DIED INSIDE BACKROOMS
                    // Respawn back into backrooms
                    // =====================================

                    if (oldPlayer.getWorld().getRegistryKey()
                            == BackroomsDimension.BACKROOMS_LEVEL_KEY) {

                        teleportToBackrooms(newPlayer);

                        return;
                    }

                    // =====================================
                    // NORMAL DEATH TELEPORT CHANCE
                    // =====================================

                    if (!RESPAWN_TO_BACKROOMS.contains(
                            newPlayer.getUuid()
                    )) {
                        return;
                    }

                    RESPAWN_TO_BACKROOMS.remove(
                            newPlayer.getUuid()
                    );

                    teleportToBackrooms(newPlayer);
                }
        );

        // =========================================
        // DAMAGE -> 5%
        // =========================================

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (entity, source, amount) -> {

                    if (!(entity instanceof ServerPlayerEntity player)) {
                        return true;
                    }

                    if (isInBackrooms(player)) {
                        return true;
                    }

                    boolean validDamage =

                            source == player.getDamageSources().fall()

                                    ||

                                    source.getAttacker() != null;

                    if (!validDamage) {
                        return true;
                    }

                    if (RANDOM.nextFloat() < 0.05f) {

                        teleportToBackrooms(player);
                    }

                    return true;
                }
        );

        // =========================================
        // SLEEP -> 20%
        // =========================================

        EntitySleepEvents.STOP_SLEEPING.register(
                (entity, sleepingPos) -> {

                    if (!(entity instanceof ServerPlayerEntity player)) {
                        return;
                    }

                    if (isInBackrooms(player)) {
                        return;
                    }

                    if (RANDOM.nextFloat() < 0.20f) {

                        teleportToBackrooms(player);
                    }
                }
        );
    }

    // =========================================
    // PORTAL CHANCE
    // =========================================

    public static void tryPortalTeleport(
            ServerPlayerEntity player
    ) {

        if (isInBackrooms(player)) {
            return;
        }

        // 10%
        if (RANDOM.nextFloat() < 0.10f) {

            teleportToBackrooms(player);
        }
    }

    // =========================================
    // TELEPORT
    // =========================================

    private static void teleportToBackrooms(
            ServerPlayerEntity player
    ) {

        ServerWorld backroomsWorld =
                player.getServer()
                        .getWorld(
                                BackroomsDimension.BACKROOMS_LEVEL_KEY
                        );

        if (backroomsWorld == null) {
            return;
        }

        player.teleport(
                backroomsWorld,

                0.5,
                80,
                0.5,

                player.getYaw(),
                player.getPitch()
        );
    }

    // =========================================
    // CHECK DIMENSION
    // =========================================

    private static boolean isInBackrooms(
            ServerPlayerEntity player
    ) {

        return player.getWorld()
                .getRegistryKey()
                == BackroomsDimension.BACKROOMS_LEVEL_KEY;
    }
}
