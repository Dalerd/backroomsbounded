package net.dalerd.backroomsbounded.block.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.GlowLichenBlock;
import net.minecraft.item.ItemPlacementContext;

public class BacteriaVineBlock extends GlowLichenBlock {

    public BacteriaVineBlock(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        // Only replace if the item being placed is NOT a bacteria vine
        // This prevents the vine from replacing solid blocks
        return !context.getStack().isOf(this.asItem()) && super.canReplace(state, context);
    }
}