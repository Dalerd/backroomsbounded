package net.dalerd.backroomsbounded.block;

import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GlitchBlock extends Block {

    // Prevent teleport spam
    private static final Map<UUID, Long> TELEPORT_COOLDOWN = new HashMap<>();

    // Cooldown in ticks (3 seconds)
    private static final long COOLDOWN_TICKS = 60;

    public GlitchBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onEntityCollision(BlockState state,
                                  World world,
                                  BlockPos pos,
                                  Entity entity) {

        // Server only
        if (world.isClient()) {
            return;
        }

        // Players only
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        UUID uuid = player.getUuid();

        long currentTime = world.getTime();

        // Prevent teleport spam
        if (TELEPORT_COOLDOWN.containsKey(uuid)) {

            long lastTeleport = TELEPORT_COOLDOWN.get(uuid);

            if (currentTime - lastTeleport < COOLDOWN_TICKS) {
                return;
            }
        }

        TELEPORT_COOLDOWN.put(uuid, currentTime);

        // Remove glitch block BEFORE teleporting
        world.removeBlock(pos, false);

        teleportToBackrooms(player);
    }

    private void teleportToBackrooms(ServerPlayerEntity player) {

        if (player.getServer() == null) {
            return;
        }

        ServerWorld backroomsWorld =
                player.getServer().getWorld(
                        BackroomsDimension.BACKROOMS_LEVEL_KEY
                );

        // Safety check
        if (backroomsWorld == null) {
            return;
        }

        // Placeholder spawn position
        double x = 0.5;
        double y = 5;
        double z = 0.5;

        player.teleport(
                backroomsWorld,
                x,
                y,
                z,
                player.getYaw(),
                player.getPitch()
        );
    }
}
