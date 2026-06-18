package net.dalerd.backroomsbounded.block.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class BacteriaShroomVerticalBlock extends Block {

    private static final VoxelShape SHAPE_NORTH =
            Block.createCuboidShape(0, 0, 0, 16, 16, 6);

    private static final VoxelShape SHAPE_SOUTH =
            Block.createCuboidShape(0, 0, 10, 16, 16, 16);

    private static final VoxelShape SHAPE_EAST =
            Block.createCuboidShape(10, 0, 0, 16, 16, 16);

    private static final VoxelShape SHAPE_WEST =
            Block.createCuboidShape(0, 0, 0, 6, 16, 16);

    public BacteriaShroomVerticalBlock(Settings settings) {
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

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();

        if (side.getAxis().isHorizontal()) {
            return this.getDefaultState()
                    .with(Properties.HORIZONTAL_FACING, side.getOpposite());
        }

        return null;
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        Direction facing = state.get(Properties.HORIZONTAL_FACING);

        BlockPos supportPos = pos.offset(facing.getOpposite());

        return world.getBlockState(supportPos)
                .isSolidBlock(world, supportPos);
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
        Direction facing = state.get(Properties.HORIZONTAL_FACING);

        if (direction == facing.getOpposite()
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

    @Override
    protected VoxelShape getOutlineShape(
            BlockState state,
            BlockView world,
            BlockPos pos,
            ShapeContext context
    ) {
        return switch (state.get(Properties.HORIZONTAL_FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockView world,
            BlockPos pos,
            ShapeContext context
    ) {
        return getOutlineShape(state, world, pos, context);
    }
}