package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.block.ModBlocks;
import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class BlockGlitchHandler {

    private static final Random RANDOM = new Random();

    private static final Map<BlockPos, GlitchData> GLITCHED_BLOCKS = new HashMap<>();
    private static final Set<BlockPos> SPREADING_GLITCHES = new HashSet<>();

    private static final double GLITCH_SPREAD_CHANCE = 0.001; // 0.1% per block per check
    private static final int GLITCH_SPREAD_INTERVAL = 100; // Check every 5 seconds
    private static final int GLITCH_LIFETIME = 200; // 10 seconds before reverting
    private static final int SPREAD_RADIUS = 3; // Spread to blocks within 3 blocks

    public static void register() {

        // =====================================
        // BLOCK BREAKING - Overworld glitches
        // =====================================
        PlayerBlockBreakEvents.AFTER.register(
                (world, player, pos, state, blockEntity) -> {
                    if (world.isClient()) return;

                    // NORMAL WORLD GLITCHES
                    if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) {
                        if (RANDOM.nextFloat() > 0.03f) return;

                        BlockState originalState = state;
                        world.setBlockState(pos, ModBlocks.GLITCH_BLOCK.getDefaultState());
                        GLITCHED_BLOCKS.put(pos, new GlitchData(originalState, world.getTime()));

                        if (player instanceof ServerPlayerEntity serverPlayer) {
                            if (RANDOM.nextFloat() < 0.35f) {
                                ServerWorld backroomsWorld = serverPlayer.getServer()
                                        .getWorld(BackroomsDimension.BACKROOMS_LEVEL_KEY);
                                if (backroomsWorld != null) {
                                    serverPlayer.teleport(backroomsWorld, 0.5, 80, 0.5,
                                            serverPlayer.getYaw(), serverPlayer.getPitch());
                                }
                            }
                        }
                    }
                }
        );

        // =====================================
        // TOUCH/INTERACT WITH GLITCH BLOCK - Backrooms escape
        // =====================================
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);

            if (state.isOf(ModBlocks.GLITCH_BLOCK) && player instanceof ServerPlayerEntity serverPlayer) {
                float roll = RANDOM.nextFloat();

                if (roll < 0.80f) {
                    // 80% - Teleport to overworld
                    ServerWorld overworld = serverPlayer.getServer().getOverworld();
                    serverPlayer.teleport(overworld, 0.5, 100, 0.5,
                            serverPlayer.getYaw(), serverPlayer.getPitch());
                } else {
                    // 20% - Fail and revert to original block
                    GlitchData data = GLITCHED_BLOCKS.get(pos);
                    if (data != null) {
                        world.setBlockState(pos, data.originalState);
                        GLITCHED_BLOCKS.remove(pos);
                    } else {
                        // If no original data, just remove glitch
                        world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    }
                }

                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });

        // =====================================
        // GLITCH SPREADING IN BACKROOMS
        // =====================================
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) continue;

                // Spread glitches
                if (world.getTime() % GLITCH_SPREAD_INTERVAL == 0) {
                    spreadGlitches(world);
                }

                // Clean up expired glitches
                cleanupGlitches(world);
            }
        });
    }

    private static void spreadGlitches(ServerWorld world) {
        List<BlockPos> newGlitches = new ArrayList<>();

        world.getPlayers().forEach(player -> {
            BlockPos playerPos = player.getBlockPos();

            // Check blocks around player
            for (int dx = -SPREAD_RADIUS; dx <= SPREAD_RADIUS; dx++) {
                for (int dy = -SPREAD_RADIUS; dy <= SPREAD_RADIUS; dy++) {
                    for (int dz = -SPREAD_RADIUS; dz <= SPREAD_RADIUS; dz++) {
                        BlockPos pos = playerPos.add(dx, dy, dz);
                        BlockState state = world.getBlockState(pos);

                        // Skip air, existing glitches, and unbreakable blocks
                        if (state.isAir()) continue;
                        if (state.isOf(ModBlocks.GLITCH_BLOCK)) continue;
                        if (state.getHardness(world, pos) < 0) continue; // Unbreakable

                        // 0.1% chance per block
                        if (RANDOM.nextFloat() < GLITCH_SPREAD_CHANCE) {
                            newGlitches.add(pos);
                        }
                    }
                }
            }
        });

        // Apply new glitches
        for (BlockPos pos : newGlitches) {
            BlockState originalState = world.getBlockState(pos);
            world.setBlockState(pos, ModBlocks.GLITCH_BLOCK.getDefaultState());
            GLITCHED_BLOCKS.put(pos, new GlitchData(originalState, world.getTime()));
        }
    }

    private static void cleanupGlitches(ServerWorld world) {
        long currentTime = world.getTime();

        GLITCHED_BLOCKS.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            GlitchData data = entry.getValue();

            // Revert after lifetime expires
            if (currentTime - data.timePlaced >= GLITCH_LIFETIME) {
                if (world.getBlockState(pos).isOf(ModBlocks.GLITCH_BLOCK)) {
                    world.setBlockState(pos, data.originalState);
                }
                return true;
            }
            return false;
        });
    }

    // =========================================
    // TICK - Called from main mod class for overworld glitch cleanup
    // =========================================
    public static void tick(ServerWorld world) {
        long currentTime = world.getTime();

        GLITCHED_BLOCKS.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            GlitchData data = entry.getValue();

            if (currentTime - data.timePlaced >= 60) {
                if (world.getBlockState(pos).isOf(ModBlocks.GLITCH_BLOCK)) {
                    world.setBlockState(pos, data.originalState);
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
