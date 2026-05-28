package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.block.ModBlocks;
import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BlockGlitchHandler {

    private static final Random RANDOM = new Random();

    private static final Map<BlockPos, GlitchData> GLITCHED_BLOCKS =
            new HashMap<>();

    public static void register() {

        PlayerBlockBreakEvents.AFTER.register(
                (world, player, pos, state, blockEntity) -> {

                    // SERVER ONLY
                    if (world.isClient()) {
                        return;
                    }

                    // =====================================
                    // NORMAL WORLD GLITCHES
                    // =====================================

                    if (world.getRegistryKey()
                            != BackroomsDimension.BACKROOMS_LEVEL_KEY) {

                        // 3% chance
                        if (RANDOM.nextFloat() > 0.03f) {
                            return;
                        }

                        BlockState originalState = state;

                        // PLACE GLITCH BLOCK
                        world.setBlockState(
                                pos,
                                ModBlocks.GLITCH_BLOCK.getDefaultState()
                        );

                        // SAVE ORIGINAL
                        GLITCHED_BLOCKS.put(
                                pos,
                                new GlitchData(
                                        originalState,
                                        world.getTime()
                                )
                        );

                        // 35% teleport chance
                        if (player instanceof ServerPlayerEntity serverPlayer) {

                            if (RANDOM.nextFloat() < 0.35f) {

                                ServerWorld backroomsWorld =
                                        serverPlayer.getServer()
                                                .getWorld(
                                                        BackroomsDimension.BACKROOMS_LEVEL_KEY
                                                );

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
                    }

                    // =====================================
                    // BACKROOMS GLITCH BLOCKS
                    // =====================================

                    else {

                        // ONLY ACTUAL GLITCH BLOCKS
                        if (!state.isOf(ModBlocks.GLITCH_BLOCK)) {
                            return;
                        }

                        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                            return;
                        }

                        // 50/50
                        boolean escape =
                                RANDOM.nextBoolean();

                        // =================================
                        // ESCAPE TO OVERWORLD
                        // =================================

                        if (escape) {

                            ServerWorld overworld =
                                    serverPlayer.getServer()
                                            .getOverworld();

                            serverPlayer.teleport(
                                    overworld,

                                    0.5,
                                    100,
                                    0.5,

                                    serverPlayer.getYaw(),
                                    serverPlayer.getPitch()
                            );
                        }

                        // =================================
                        // TELEPORT TO RANDOM BACKROOMS SPAWN
                        // =================================

                        else {

                            ServerWorld backrooms =
                                    serverPlayer.getServer()
                                            .getWorld(
                                                    BackroomsDimension.BACKROOMS_LEVEL_KEY
                                            );

                            if (backrooms == null) {
                                return;
                            }

                            serverPlayer.teleport(
                                    backrooms,

                                    0.5,
                                    80,
                                    0.5,

                                    serverPlayer.getYaw(),
                                    serverPlayer.getPitch()
                            );
                        }
                    }
                }
        );
    }

    // =========================================
    // TICK
    // =========================================

    public static void tick(ServerWorld world) {

        long currentTime =
                world.getTime();

        GLITCHED_BLOCKS.entrySet().removeIf(entry -> {

            BlockPos pos =
                    entry.getKey();

            GlitchData data =
                    entry.getValue();

            // REMOVE AFTER 60 TICKS
            if (currentTime - data.timePlaced >= 60) {

                if (world.getBlockState(pos)
                        .isOf(ModBlocks.GLITCH_BLOCK)) {

                    world.setBlockState(
                            pos,
                            data.originalState
                    );
                }

                return true;
            }

            return false;
        });
    }

    // =========================================
    // DATA
    // =========================================

    private record GlitchData(
            BlockState originalState,
            long timePlaced
    ) {}
}
