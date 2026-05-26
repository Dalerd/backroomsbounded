package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.block.ModBlocks;
import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BlockGlitchHandler {

    private static final Random RANDOM = new Random();

    private static final Map<BlockPos, GlitchData> GLITCHED_BLOCKS = new HashMap<>();

    public static void register() {

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {

            // ONLY SERVER SIDE
            if (world.isClient()) {
                return;
            }

            // DO NOT ALLOW GLITCHES IN BACKROOMS
            if (world.getRegistryKey() == BackroomsDimension.BACKROOMS_LEVEL_KEY) {
                return;
            }

            // SMALL RANDOM CHANCE
            if (RANDOM.nextFloat() > 0.03f) {
                return;
            }

            BlockState originalState = state;

            // PLACE GLITCH BLOCK
            world.setBlockState(pos, ModBlocks.GLITCH_BLOCK.getDefaultState());

            // SAVE ORIGINAL BLOCK
            GLITCHED_BLOCKS.put(
                    pos,
                    new GlitchData(originalState, world.getTime())
            );

            // TELEPORT CHANCE
            if (player instanceof ServerPlayerEntity serverPlayer) {

                if (RANDOM.nextFloat() < 0.35f) {

                    ServerWorld backroomsWorld =
                            serverPlayer.getServer()
                                    .getWorld(BackroomsDimension.BACKROOMS_LEVEL_KEY);

                    if (backroomsWorld != null) {

                        serverPlayer.teleport(
                                backroomsWorld,
                                0.5,
                                80,
                                0.5,
                                serverPlayer.getYaw(),
                                serverPlayer.getPitch()
                        );
                    }
                }
            }
        });
    }

    public static void tick(ServerWorld world) {

        long currentTime = world.getTime();

        GLITCHED_BLOCKS.entrySet().removeIf(entry -> {

            BlockPos pos = entry.getKey();
            GlitchData data = entry.getValue();

            // REMOVE AFTER 60 TICKS
            if (currentTime - data.timePlaced >= 60) {

                if (world.getBlockState(pos).isOf(ModBlocks.GLITCH_BLOCK)) {

                    world.setBlockState(pos, data.originalState);
                }

                return true;
            }

            return false;
        });
    }

    private record GlitchData(
            BlockState originalState,
            long timePlaced
    ) {}
}
