package net.dalerd.backroomsbounded.sanity;

import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.TypedActionResult;

public class SanityFoodHandler {

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return TypedActionResult.pass(player.getStackInHand(hand));

            // Only in backrooms
            if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY)
                return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);

            // Check if it's cooked food
            if (isCookedFood(stack)) {
                // Reduce panic when eating cooked food
                SanityManager.reducePanic(serverPlayer, 15);
            }

            return TypedActionResult.pass(stack);
        });
    }

    private static boolean isCookedFood(ItemStack stack) {
        return stack.isOf(Items.COOKED_BEEF) ||
                stack.isOf(Items.COOKED_PORKCHOP) ||
                stack.isOf(Items.COOKED_CHICKEN) ||
                stack.isOf(Items.COOKED_MUTTON) ||
                stack.isOf(Items.COOKED_RABBIT) ||
                stack.isOf(Items.COOKED_COD) ||
                stack.isOf(Items.COOKED_SALMON) ||
                stack.isOf(Items.BREAD) ||
                stack.isOf(Items.BAKED_POTATO) ||
                stack.isOf(Items.MUSHROOM_STEW) ||
                stack.isOf(Items.BEETROOT_SOUP) ||
                stack.isOf(Items.RABBIT_STEW) ||
                stack.isOf(Items.PUMPKIN_PIE) ||
                stack.isOf(Items.GOLDEN_CARROT) ||
                stack.isOf(Items.GOLDEN_APPLE);
    }
}
