package net.dalerd.backroomsbounded.block.custom;

import com.mojang.serialization.MapCodec;

import net.dalerd.backroomsbounded.block.entity.ModBlockEntities;
import net.dalerd.backroomsbounded.block.entity.WaterCoolerBlockEntity;
import net.dalerd.backroomsbounded.item.ModItems;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.BlockRenderType;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;

import net.minecraft.entity.player.PlayerEntity;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;

import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import net.minecraft.world.World;

public class WaterCoolerBlock extends BlockWithEntity {

    public static final MapCodec<WaterCoolerBlock> CODEC =
            createCodec(WaterCoolerBlock::new);

    public static final DirectionProperty FACING =
            Properties.HORIZONTAL_FACING;

    public WaterCoolerBlock(Settings settings) {
        super(settings);

        setDefaultState(
                getStateManager().getDefaultState()
                        .with(FACING, Direction.NORTH)
        );
    }

    @Override
    public MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(
            StateManager.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new WaterCoolerBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit
    ) {

        ItemStack heldItem = player.getMainHandStack();

        if (heldItem.isOf(Items.GLASS_BOTTLE)) {

            BlockEntity be = world.getBlockEntity(pos);

            if (be instanceof WaterCoolerBlockEntity cooler) {

                if (cooler.hasServings()) {

                    if (!world.isClient()) {

                        cooler.takeServing();

                        heldItem.decrement(1);

                        ItemStack almondWater =
                                new ItemStack(ModItems.ALMOND_WATER);

                        if (!player.getInventory().insertStack(almondWater)) {
                            player.dropItem(almondWater, false);
                        }
                    }

                    return ActionResult.SUCCESS;
                }
            }
        }

        return ActionResult.PASS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world,
            BlockState state,
            BlockEntityType<T> type
    ) {

        return validateTicker(
                type,
                ModBlockEntities.WATER_COOLER_BE,
                WaterCoolerBlockEntity::tick
        );
    }
}
