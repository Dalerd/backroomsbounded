package net.dalerd.backroomsbounded.block.custom;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class BacteriaShroomHorizontalBlock extends Block {

    private static final VoxelShape SHAPE =
            Block.createCuboidShape(1, 0, 1, 15, 5, 15);

    public BacteriaShroomHorizontalBlock(Settings settings) {
        super(settings);

        this.setDefaultState(
                this.stateManager.getDefaultState()
                        .with(Properties.HORIZONTAL_FACING, Direction.NORTH)
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction randomFacing = Direction.Type.HORIZONTAL.random(Random.create());

        return this.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, randomFacing);
    }

    @Override
    protected VoxelShape getOutlineShape(
            BlockState state,
            BlockView world,
            BlockPos pos,
            ShapeContext context
    ) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockView world,
            BlockPos pos,
            ShapeContext context
    ) {
        return SHAPE;
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos belowPos = pos.down();

        return world.getBlockState(belowPos)
                .isSolidBlock(world, belowPos);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            WorldAccess world,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (direction == Direction.DOWN
                && !neighborState.isSolidBlock(world, neighborPos)) {
            return Blocks.AIR.getDefaultState();
        }

        return super.getStateForNeighborUpdate(
                state,
                direction,
                neighborState,
                world,
                pos,
                neighborPos
        );
    }
}