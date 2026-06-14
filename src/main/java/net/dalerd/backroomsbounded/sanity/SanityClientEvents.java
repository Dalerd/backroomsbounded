package net.dalerd.backroomsbounded.sanity;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import org.joml.Matrix4f;

import java.util.*;

public class SanityClientEvents {

    private static int tickCounter = 0;
    private static final Map<UUID, Integer> heartbeatCooldowns = new HashMap<>();
    private static final Map<UUID, Integer> fakeSoundCooldowns = new HashMap<>();

    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;

            if (player == null) return;
            if (!isInBackrooms(player)) return;

            int panic = getClientPanic();
            if (panic > SanityManager.CALM) {
                renderPanicOverlay(drawContext.getMatrices(), panic);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tickCounter++;

            ClientPlayerEntity player = client.player;
            if (player == null) return;
            if (!isInBackrooms(player)) return;

            int panic = getClientPanic();

            if (panic >= SanityManager.UNEASY) {
                tryPlayHeartbeat(player, panic);
            }

            if (panic >= SanityManager.ANXIOUS) {
                tryPlayFakeSound(player, panic);
            }
        });
    }

    private static boolean isInBackrooms(ClientPlayerEntity player) {
        return player.getWorld().getRegistryKey().getValue()
                .equals(Identifier.of("backroomsbounded", "backrooms"));
    }

    private static int getClientPanic() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return 0;

        int lightLevel = client.player.getWorld().getLightLevel(
                net.minecraft.world.LightType.BLOCK,
                client.player.getBlockPos()
        );

        if (lightLevel <= 4) {
            return Math.min(100, tickCounter / 2);
        } else {
            return Math.max(0, tickCounter / 10);
        }
    }

    private static void renderPanicOverlay(MatrixStack matrices, int panic) {
        float opacity = (panic - SanityManager.CALM) / (float)(SanityManager.MAX_PANIC - SanityManager.CALM);
        opacity = Math.min(0.7f, opacity * 0.7f);

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, 0, 0, 0).color(0, 0, 0, opacity);
        buffer.vertex(matrix, screenWidth, 0, 0).color(0, 0, 0, opacity);
        buffer.vertex(matrix, screenWidth, screenHeight, 0).color(0, 0, 0, opacity);
        buffer.vertex(matrix, 0, screenHeight, 0).color(0, 0, 0, opacity);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void tryPlayHeartbeat(ClientPlayerEntity player, int panic) {
        UUID playerId = player.getUuid();
        int cooldown = heartbeatCooldowns.getOrDefault(playerId, 0);

        if (cooldown > 0) {
            heartbeatCooldowns.put(playerId, cooldown - 1);
            return;
        }

        int baseCooldown = 40;
        int adjustedCooldown = baseCooldown - ((panic - 50) / 5);
        adjustedCooldown = Math.max(10, adjustedCooldown);

        player.getWorld().playSound(
                player,
                player.getBlockPos(),
                SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(),
                SoundCategory.PLAYERS,
                0.2f,
                0.5f
        );

        heartbeatCooldowns.put(playerId, adjustedCooldown);
    }

    private static void tryPlayFakeSound(ClientPlayerEntity player, int panic) {
        UUID playerId = player.getUuid();
        int cooldown = fakeSoundCooldowns.getOrDefault(playerId, 0);

        if (cooldown > 0) {
            fakeSoundCooldowns.put(playerId, cooldown - 1);
            return;
        }

        ChunkRandom random = new ChunkRandom(new CheckedRandom(
                playerId.hashCode() + tickCounter
        ));

        // Only 8% chance per check to play a sound (was 10% before)
        if (random.nextFloat() < 0.08f) {
            // Creepy distant sounds only - no constant footsteps
            SoundEvent[] creepySounds = {
                    SoundEvents.BLOCK_IRON_DOOR_OPEN,
                    SoundEvents.BLOCK_IRON_DOOR_CLOSE,
                    SoundEvents.BLOCK_CHEST_OPEN,
                    SoundEvents.BLOCK_BARREL_OPEN,
                    SoundEvents.BLOCK_BARREL_CLOSE,
                    SoundEvents.ENTITY_ENDERMAN_STARE,
                    SoundEvents.ENTITY_ENDERMAN_SCREAM,
                    SoundEvents.BLOCK_STONE_STEP,
                    SoundEvents.AMBIENT_CAVE.value()
            };

            SoundEvent chosenSound = creepySounds[random.nextInt(creepySounds.length)];

            // Play sound at random distance (4-20 blocks away)
            int offsetX = random.nextInt(32) - 16;
            int offsetY = random.nextInt(4) - 2;
            int offsetZ = random.nextInt(32) - 16;

            // Sometimes play "running" footsteps that move closer
            if (random.nextFloat() < 0.2f && panic > 80) {
                // Play 2-3 quick footsteps getting closer
                for (int i = 0; i < 3; i++) {
                    float vol = 0.1f + (i * 0.1f);
                    float pitch = 0.6f + (i * 0.2f);
                    int stepOffset = 15 - (i * 4);

                    player.getWorld().playSound(
                            player,
                            player.getBlockPos().add(
                                    (int)(Math.cos(random.nextFloat() * 6.28) * stepOffset),
                                    0,
                                    (int)(Math.sin(random.nextFloat() * 6.28) * stepOffset)
                            ),
                            SoundEvents.BLOCK_STONE_STEP,
                            SoundCategory.AMBIENT,
                            vol,
                            pitch
                    );
                }
            } else {
                // Single distant sound
                player.getWorld().playSound(
                        player,
                        player.getBlockPos().add(offsetX, offsetY, offsetZ),
                        chosenSound,
                        SoundCategory.AMBIENT,
                        0.2f,
                        0.8f + random.nextFloat() * 0.4f
                );
            }
        }

        // Long cooldown between sounds (3-10 seconds)
        int newCooldown = 60 + random.nextInt(140);
        fakeSoundCooldowns.put(playerId, newCooldown);
    }
}