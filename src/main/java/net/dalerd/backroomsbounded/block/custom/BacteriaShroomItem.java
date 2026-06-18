package net.dalerd.backroomsbounded.block.custom;

import net.dalerd.backroomsbounded.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.Direction;

public class BacteriaShroomItem extends BlockItem {

    public BacteriaShroomItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    protected BlockState getPlacementState(ItemPlacementContext context) {
        Direction side = context.getSide();

        // If placing on a wall (horizontal side), use vertical block
        if (side.getAxis().isHorizontal()) {
            return ModBlocks.BACTERIA_SHROOM_VERTICAL.getPlacementState(context);
        }

        // If placing on top of a block, use horizontal block
        if (side == Direction.UP) {
            return ModBlocks.BACTERIA_SHROOM_HORIZONTAL.getDefaultState();
        }

        // Don't allow placing on bottom
        return null;
    }
}