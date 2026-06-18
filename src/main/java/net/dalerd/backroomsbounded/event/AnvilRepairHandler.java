package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;

public class AnvilRepairHandler {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) return ActionResult.PASS;

            // Check if player is using an anvil
            if (!world.getBlockState(hitResult.getBlockPos()).isOf(Blocks.ANVIL) &&
                    !world.getBlockState(hitResult.getBlockPos()).isOf(Blocks.CHIPPED_ANVIL) &&
                    !world.getBlockState(hitResult.getBlockPos()).isOf(Blocks.DAMAGED_ANVIL)) {
                return ActionResult.PASS;
            }

            // Check all armor slots for diamond/netherite that has been damaged
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;

                ItemStack armor = player.getEquippedStack(slot);
                if (armor.isEmpty()) continue;

                if (isDiamondOrNetherite(armor) && armor.getDamage() > 0) {
                    // Armor has been damaged by backrooms decay - break it completely
                    player.getInventory().armor.set(slot.getEntitySlotId(), ItemStack.EMPTY);
                }
            }

            return ActionResult.PASS;
        });
    }

    private static boolean isDiamondOrNetherite(ItemStack stack) {
        return stack.isOf(Items.DIAMOND_HELMET) || stack.isOf(Items.DIAMOND_CHESTPLATE) ||
                stack.isOf(Items.DIAMOND_LEGGINGS) || stack.isOf(Items.DIAMOND_BOOTS) ||
                stack.isOf(Items.NETHERITE_HELMET) || stack.isOf(Items.NETHERITE_CHESTPLATE) ||
                stack.isOf(Items.NETHERITE_LEGGINGS) || stack.isOf(Items.NETHERITE_BOOTS);
    }
}