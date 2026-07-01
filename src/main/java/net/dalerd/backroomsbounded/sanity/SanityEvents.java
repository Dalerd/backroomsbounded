package net.dalerd.backroomsbounded.sanity;

import net.dalerd.backroomsbounded.advancement.AdvancementTriggerHandler;
import net.dalerd.backroomsbounded.sound.ModSounds;
import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.LightType;

import java.util.*;

public class SanityEvents {

    private static final Random RANDOM = new Random();
    private static int tickCounter = 0;
    private static final int PANIC_CHECK_INTERVAL = 100;

    private static final Map<UUID, Integer> heartbeatCooldowns = new HashMap<>();
    private static final int HEARTBEAT_COOLDOWN = 60;

    // Track which players already got the panic attack advancement
    private static final Set<UUID> panicAdvancementGranted = new HashSet<>();

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) {
                SanityManager.onPlayerDeath(player);
                heartbeatCooldowns.remove(player.getUuid());
                panicAdvancementGranted.remove(player.getUuid()); // Reset on death so they can earn it again
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            heartbeatCooldowns.entrySet().removeIf(entry -> {
                entry.setValue(entry.getValue() - 1);
                return entry.getValue() <= 0;
            });

            if (tickCounter % PANIC_CHECK_INTERVAL != 0) return;

            for (ServerWorld world : server.getWorlds()) {
                if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) continue;

                for (ServerPlayerEntity player : world.getPlayers()) {
                    updatePanic(player);
                }
            }
        });
    }

    private static void updatePanic(ServerPlayerEntity player) {
        int lightLevel = player.getWorld().getLightLevel(LightType.BLOCK, player.getBlockPos());

        if (lightLevel <= 4) {
            SanityManager.incrementDarknessTicks(player);

            if (SanityManager.getDarknessTicks(player) >= SanityManager.DARKNESS_PANIC_THRESHOLD) {
                if (lightLevel == 0) {
                    SanityManager.addPanic(player, 2);
                } else {
                    SanityManager.addPanic(player, 1);
                }
            }
        } else if (lightLevel >= 8) {
            SanityManager.resetDarknessTicks(player);
            SanityManager.reducePanic(player, 1);
        }

        int panic = SanityManager.getPanic(player);
        if (panic < SanityManager.MIN_PANIC) SanityManager.setPanic(player, SanityManager.MIN_PANIC);
        if (panic > SanityManager.MAX_PANIC) SanityManager.setPanic(player, SanityManager.MAX_PANIC);

        // Heartbeat sound at high panic (70+)
        if (panic >= 70 && !heartbeatCooldowns.containsKey(player.getUuid())) {
            float volume = panic >= 90 ? 1.0f : 0.6f;
            float pitch = 0.8f + (panic - 70) * 0.01f;
            player.playSoundToPlayer(ModSounds.AMBIENT_HEARTBEAT,
                    SoundCategory.PLAYERS, volume, pitch);
            heartbeatCooldowns.put(player.getUuid(), HEARTBEAT_COOLDOWN);
        }

        // Grant panic attack advancement at 90+
        if (panic >= 90 && !panicAdvancementGranted.contains(player.getUuid())) {
            AdvancementTriggerHandler.onPanicAttack(player);
            panicAdvancementGranted.add(player.getUuid());
        }

        // Apply random panic effects at high panic (81-100)
        if (panic >= 81 && RANDOM.nextFloat() < 0.10f) {
            applyRandomPanicEffect(player);
        }
    }

    private static void applyRandomPanicEffect(ServerPlayerEntity player) {
        int effect = RANDOM.nextInt(5);
        switch (effect) {
            case 0 -> player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.BLINDNESS, 100, 0, false, false));
            case 1 -> player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.DARKNESS, 100, 0, false, false));
            case 2 -> player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NAUSEA, 100, 0, false, false));
            case 3 -> player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS, 100, 0, false, false));
            case 4 -> player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SPEED, 100, 0, false, false));
        }
    }
}