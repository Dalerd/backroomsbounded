package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.block.ModBlocks;
import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.util.math.BlockPos;

import java.util.Random;

public class FroglightCorruptionHandler {

    private static final Random RANDOM = new Random();

    public static void register() {

        ServerTickEvents.END_WORLD_TICK.register(world -> {

            // RUN ONLY EVERY 20 TICKS (1 SECOND)
            if (world.getTime() % 20 != 0) {
                return;
            }

            // ONLY BACKROOMS DIMENSION
            if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) {
                return;
            }

            tickFroglights(world);
        });
    }

    private static void tickFroglights(ServerWorld world) {

        for (ServerPlayerEntity player : world.getPlayers()) {

            BlockPos playerPos = player.getBlockPos();

            int x = playerPos.getX() + RANDOM.nextInt(64) - 32;
            int y = playerPos.getY() + RANDOM.nextInt(20) - 10;
            int z = playerPos.getZ() + RANDOM.nextInt(64) - 32;

            BlockPos pos = new BlockPos(x, y, z);

            BlockState state = world.getBlockState(pos);

            boolean isFroglight =
                    state.isOf(Blocks.OCHRE_FROGLIGHT) ||
                            state.isOf(Blocks.PEARLESCENT_FROGLIGHT) ||
                            state.isOf(Blocks.VERDANT_FROGLIGHT);

            if (!isFroglight) {
                continue;
            }

            // 30% CHANCE
            if (RANDOM.nextFloat() < 0.30f) {

                world.setBlockState(
                        pos,
                        ModBlocks.BACKBOARD_BLOCK.getDefaultState(),
                        3
                );
            }
        }
    }
}
