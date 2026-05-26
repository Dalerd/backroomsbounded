package net.dalerd.backroomsbounded.item.custom;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import net.minecraft.world.World;

public class AlmondWaterItem extends Item {

    public AlmondWaterItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(
            ItemStack stack,
            World world,
            LivingEntity user
    ) {

        ItemStack result = super.finishUsing(stack, world, user);

        if (user instanceof PlayerEntity player) {

            // heal 1 heart
            player.heal(2.0f);
        }

        return result;
    }
}
