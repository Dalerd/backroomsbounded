package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class ArmorDecayHandler {

    private static int tickCounter = 0;
    private static final int DECAY_CHECK_INTERVAL = 40; // Every 2 seconds

    // Time to break (in ticks) - 20 ticks = 1 second
    private static final int DIAMOND_BREAK_TIME = 9600;  // 8 minutes
    private static final int NETHERITE_BREAK_TIME = 12000; // 10 minutes

    // Damage per check interval
    private static final int DIAMOND_DAMAGE_PER_CHECK =
            (DIAMOND_BREAK_TIME / DECAY_CHECK_INTERVAL);
    private static final int NETHERITE_DAMAGE_PER_CHECK =
            (NETHERITE_BREAK_TIME / DECAY_CHECK_INTERVAL);

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            if (tickCounter % DECAY_CHECK_INTERVAL != 0) return;

            for (ServerWorld world : server.getWorlds()) {
                if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) continue;

                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (player.isCreative() || player.isSpectator()) continue;
                    decayArmor(player);
                }
            }
        });
    }

    private static void decayArmor(ServerPlayerEntity player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;

            ItemStack armor = player.getEquippedStack(slot);
            if (armor.isEmpty()) continue;

            boolean isDiamond = isDiamondArmor(armor, slot);
            boolean isNetherite = isNetheriteArmor(armor, slot);

            if (!isDiamond && !isNetherite) continue;

            int damageAmount;
            if (isNetherite) {
                damageAmount = NETHERITE_DAMAGE_PER_CHECK;
            } else {
                damageAmount = DIAMOND_DAMAGE_PER_CHECK;
            }

            // Mending = 4x faster breaking
            if (hasMending(armor)) {
                damageAmount *= 4;
            }

            // Apply damage
            int newDamage = armor.getDamage() + damageAmount;

            if (newDamage >= armor.getMaxDamage()) {
                // Break the armor
                player.getInventory().armor.set(slot.getEntitySlotId(), ItemStack.EMPTY);
            } else {
                armor.setDamage(newDamage);
            }
        }
    }

    private static boolean hasMending(ItemStack stack) {
        return stack.getEnchantments().getEnchantments().stream()
                .anyMatch(entry -> entry.getKey().isPresent() &&
                        entry.getKey().get().getValue().equals(Enchantments.MENDING.getValue()));
    }

    private static boolean isDiamondArmor(ItemStack stack, EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> stack.isOf(Items.DIAMOND_HELMET);
            case CHEST -> stack.isOf(Items.DIAMOND_CHESTPLATE);
            case LEGS -> stack.isOf(Items.DIAMOND_LEGGINGS);
            case FEET -> stack.isOf(Items.DIAMOND_BOOTS);
            default -> false;
        };
    }

    private static boolean isNetheriteArmor(ItemStack stack, EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> stack.isOf(Items.NETHERITE_HELMET);
            case CHEST -> stack.isOf(Items.NETHERITE_CHESTPLATE);
            case LEGS -> stack.isOf(Items.NETHERITE_LEGGINGS);
            case FEET -> stack.isOf(Items.NETHERITE_BOOTS);
            default -> false;
        };
    }
}