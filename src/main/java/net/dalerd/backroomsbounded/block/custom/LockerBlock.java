package net.dalerd.backroomsbounded.block.custom;

import com.mojang.serialization.MapCodec;

import net.dalerd.backroomsbounded.block.entity.LockerBlockEntity;
import net.dalerd.backroomsbounded.block.entity.ModBlockEntities;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;

import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;

import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import net.minecraft.util.hit.BlockHitResult;

import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class LockerBlock extends BlockWithEntity {

    public static final MapCodec<LockerBlock> CODEC =
            createCodec(LockerBlock::new);

    public static final DirectionProperty FACING =
            Properties.HORIZONTAL_FACING;

    public static final BooleanProperty OPEN =
            Properties.OPEN;

    public static final EnumProperty<DoubleBlockHalf> HALF =
            Properties.DOUBLE_BLOCK_HALF;

    private static final double WALL = 2.0;
    private static final double FLOOR = 1.0;
    private static final double CEIL = 15.0;

    public LockerBlock(Settings settings) {

        super(settings);

        this.setDefaultState(
                this.stateManager.getDefaultState()
                        .with(FACING, Direction.NORTH)
                        .with(OPEN, false)
                        .with(HALF, DoubleBlockHalf.LOWER)
        );
    }

    @Override
    public MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {

        builder.add(
                FACING,
                OPEN,
                HALF
        );
    }

    // =========================================================
    // IMPORTANT FIX
    // =========================================================

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    // =========================================================
    // SHAPE
    // =========================================================

    private VoxelShape createShape(BlockState state) {

        boolean open = state.get(OPEN);
        boolean upper = state.get(HALF) == DoubleBlockHalf.UPPER;

        VoxelShape shape = VoxelShapes.empty();

        // LEFT WALL
        shape = VoxelShapes.union(
                shape,
                Block.createCuboidShape(
                        0, 0, 0,
                        WALL, 16, 16
                )
        );

        // RIGHT WALL
        shape = VoxelShapes.union(
                shape,
                Block.createCuboidShape(
                        16 - WALL, 0, 0,
                        16, 16, 16
                )
        );

        // BACK WALL
        shape = VoxelShapes.union(
                shape,
                Block.createCuboidShape(
                        0, 0, 16 - WALL,
                        16, 16, 16
                )
        );

        // FRONT DOOR
        if (!open) {

            shape = VoxelShapes.union(
                    shape,
                    Block.createCuboidShape(
                            0, 0, 0,
                            16, 16, WALL
                    )
            );
        }

        // FLOOR
        if (!upper) {

            shape = VoxelShapes.union(
                    shape,
                    Block.createCuboidShape(
                            0, 0, 0,
                            16, FLOOR, 16
                    )
            );
        }

        // CEILING
        else {

            shape = VoxelShapes.union(
                    shape,
                    Block.createCuboidShape(
                            0, CEIL, 0,
                            16, 16, 16
                    )
            );
        }

        return rotateShape(shape, state.get(FACING));
    }

    // =========================================================
    // ROTATION
    // =========================================================

    private VoxelShape rotateShape(VoxelShape shape, Direction direction) {

        VoxelShape[] buffer = new VoxelShape[] {
                shape,
                VoxelShapes.empty()
        };

        int times = (direction.getHorizontal() + 4) % 4;

        for (int i = 0; i < times; i++) {

            buffer[1] = VoxelShapes.empty();

            buffer[0].forEachBox((minX, minY, minZ, maxX, maxY, maxZ) ->

                    buffer[1] = VoxelShapes.union(
                            buffer[1],

                            VoxelShapes.cuboid(
                                    1 - maxZ,
                                    minY,
                                    minX,

                                    1 - minZ,
                                    maxY,
                                    maxX
                            )
                    )
            );

            buffer[0] = buffer[1];
        }

        return buffer[0];
    }

    // =========================================================
    // COLLISION
    // =========================================================

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockView world,
            BlockPos pos,
            ShapeContext context
    ) {

        return createShape(state);
    }

    @Override
    public VoxelShape getOutlineShape(
            BlockState state,
            BlockView world,
            BlockPos pos,
            ShapeContext context
    ) {

        return createShape(state);
    }

    // =========================================================
    // PLACEMENT
    // =========================================================

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {

        BlockPos up = ctx.getBlockPos().up();

        if (!ctx.getWorld().getBlockState(up).isReplaceable()) {
            return null;
        }

        return this.getDefaultState()
                .with(
                        FACING,
                        ctx.getHorizontalPlayerFacing()
                )
                .with(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void onPlaced(
            World world,
            BlockPos pos,
            BlockState state,
            LivingEntity placer,
            ItemStack stack
    ) {

        world.setBlockState(
                pos.up(),

                state.with(HALF, DoubleBlockHalf.UPPER),

                Block.NOTIFY_ALL
        );
    }

    // =========================================================
    // BREAKING
    // =========================================================

    @Override
    public void onStateReplaced(
            BlockState state,
            World world,
            BlockPos pos,
            BlockState newState,
            boolean moved
    ) {

        if (state.isOf(newState.getBlock())) {
            return;
        }

        BlockPos otherPos =
                state.get(HALF) == DoubleBlockHalf.LOWER
                        ? pos.up()
                        : pos.down();

        if (world.getBlockState(otherPos).isOf(this)) {

            world.setBlockState(
                    otherPos,
                    Blocks.AIR.getDefaultState(),
                    Block.NOTIFY_ALL
            );
        }

        super.onStateReplaced(
                state,
                world,
                pos,
                newState,
                moved
        );
    }

    // =========================================================
    // INTERACTION
    // =========================================================

    @Override
    protected ActionResult onUse(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit
    ) {

        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        BlockPos basePos =
                state.get(HALF) == DoubleBlockHalf.UPPER
                        ? pos.down()
                        : pos;

        BlockState baseState =
                world.getBlockState(basePos);

        boolean open =
                !baseState.get(OPEN);

        // LOWER
        world.setBlockState(
                basePos,
                baseState.with(OPEN, open),
                Block.NOTIFY_ALL
        );

        // UPPER
        world.setBlockState(
                basePos.up(),
                world.getBlockState(basePos.up())
                        .with(OPEN, open),
                Block.NOTIFY_ALL
        );

        world.playSound(
                null,
                pos,

                open
                        ? SoundEvents.BLOCK_IRON_DOOR_OPEN
                        : SoundEvents.BLOCK_IRON_DOOR_CLOSE,

                SoundCategory.BLOCKS,

                1f,
                1f
        );

        return ActionResult.SUCCESS;
    }

    // =========================================================
    // BLOCK ENTITY
    // =========================================================

    @Override
    public BlockEntity createBlockEntity(
            BlockPos pos,
            BlockState state
    ) {

        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            return null;
        }

        return new LockerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world,
            BlockState state,
            BlockEntityType<T> type
    ) {

        return validateTicker(
                type,
                ModBlockEntities.LOCKER_BE,
                LockerBlockEntity::tick
        );
    }
}