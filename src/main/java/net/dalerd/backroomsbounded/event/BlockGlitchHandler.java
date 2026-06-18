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
import net.minecraft.util.math.ChunkPos;

import java.util.*;

public class BlockGlitchHandler {

    private static final Random RANDOM = new Random();

    private static final Map<BlockPos, GlitchData> GLITCHED_BLOCKS = new HashMap<>();
    private static final Map<ChunkPos, Integer> GLITCH_CHUNK_COOLDOWNS = new HashMap<>();

    private static final double GLITCH_SPREAD_CHANCE = 0.00005; // 0.005% per block (was 0.1%)
    private static final int GLITCH_SPREAD_INTERVAL = 200; // Check every 10 seconds (was 5)
    private static final int GLITCH_LIFETIME = 200; // 10 seconds before reverting
    private static final int SPREAD_RADIUS = 3;
    private static final int CHUNK_COOLDOWN_RADIUS = 5; // 5 chunk radius cooldown
    private static final int CHUNK_COOLDOWN_TICKS = 12000; // 10 minutes cooldown

    public static void register() {

        // =====================================
        // BLOCK BREAKING - Overworld glitches
        // =====================================
        PlayerBlockBreakEvents.AFTER.register(
                (world, player, pos, state, blockEntity) -> {
                    if (world.isClient()) return;

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
                    ServerWorld overworld = serverPlayer.getServer().getOverworld();
                    serverPlayer.teleport(overworld, 0.5, 100, 0.5,
                            serverPlayer.getYaw(), serverPlayer.getPitch());
                } else {
                    GlitchData data = GLITCHED_BLOCKS.get(pos);
                    if (data != null) {
                        world.setBlockState(pos, data.originalState);
                        GLITCHED_BLOCKS.remove(pos);
                    } else {
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

                // Decrement chunk cooldowns
                GLITCH_CHUNK_COOLDOWNS.entrySet().removeIf(entry -> {
                    entry.setValue(entry.getValue() - 1);
                    return entry.getValue() <= 0;
                });

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

            for (int dx = -SPREAD_RADIUS; dx <= SPREAD_RADIUS; dx++) {
                for (int dy = -SPREAD_RADIUS; dy <= SPREAD_RADIUS; dy++) {
                    for (int dz = -SPREAD_RADIUS; dz <= SPREAD_RADIUS; dz++) {
                        BlockPos pos = playerPos.add(dx, dy, dz);
                        BlockState state = world.getBlockState(pos);

                        if (state.isAir()) continue;
                        if (state.isOf(ModBlocks.GLITCH_BLOCK)) continue;
                        if (state.getHardness(world, pos) < 0) continue;

                        // Check if this chunk or nearby chunks are on cooldown
                        ChunkPos chunkPos = new ChunkPos(pos);
                        boolean isOnCooldown = false;
                        for (Map.Entry<ChunkPos, Integer> entry : GLITCH_CHUNK_COOLDOWNS.entrySet()) {
                            ChunkPos cooldownChunk = entry.getKey();
                            int dxc = Math.abs(chunkPos.x - cooldownChunk.x);
                            int dzc = Math.abs(chunkPos.z - cooldownChunk.z);
                            if (dxc <= CHUNK_COOLDOWN_RADIUS && dzc <= CHUNK_COOLDOWN_RADIUS) {
                                isOnCooldown = true;
                                break;
                            }
                        }

                        if (!isOnCooldown && RANDOM.nextFloat() < GLITCH_SPREAD_CHANCE) {
                            newGlitches.add(pos);
                            GLITCH_CHUNK_COOLDOWNS.put(chunkPos, CHUNK_COOLDOWN_TICKS);
                        }
                    }
                }
            }
        });

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

            if (currentTime - data.timePlaced >= GLITCH_LIFETIME) {
                if (world.getBlockState(pos).isOf(ModBlocks.GLITCH_BLOCK)) {
                    world.setBlockState(pos, data.originalState);
                }
                return true;
            }
            return false;
        });
    }

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

    private record GlitchData(
            BlockState originalState,
            long timePlaced
    ) {}
}
