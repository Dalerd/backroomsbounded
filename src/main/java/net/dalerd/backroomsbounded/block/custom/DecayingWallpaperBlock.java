package net.dalerd.backroomsbounded.block.custom;

import net.dalerd.backroomsbounded.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class DecayingWallpaperBlock extends Block {

    public DecayingWallpaperBlock(Settings settings) {
        super(settings.ticksRandomly());
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {

        // Decay from water nearby
        if (isNearWater(world, pos)) {
            decayStage(state, world, pos, random);
            return;
        }

        // Decay from bacteria blocks nearby
        if (isNearBacteria(world, pos)) {
            // Bacteria accelerates decay - higher chance
            if (random.nextInt(5) == 0) {
                decayStage(state, world, pos, random);
            }
            return;
        }
    }

    private void decayStage(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        Block current = state.getBlock();

        // NORMAL -> STAINED
        if (current == ModBlocks.WALLPAPER_BLOCK) {
            if (random.nextInt(8) == 0) {
                world.setBlockState(pos, ModBlocks.STAINED_WALLPAPER_BLOCK.getDefaultState());
            }
        }

        // STAINED -> WET
        else if (current == ModBlocks.STAINED_WALLPAPER_BLOCK) {
            if (random.nextInt(8) == 0) {
                world.setBlockState(pos, ModBlocks.WET_WALLPAPER_BLOCK.getDefaultState());
            }
        }

        // WET -> MOLDY
        else if (current == ModBlocks.WET_WALLPAPER_BLOCK) {
            if (random.nextInt(10) == 0) {
                world.setBlockState(pos, ModBlocks.MOLDY_WALLPAPER_BLOCK.getDefaultState());
            }
        }

        // MOLDY -> MOLD INFECTED
        else if (current == ModBlocks.MOLDY_WALLPAPER_BLOCK) {
            if (random.nextInt(12) == 0) {
                world.setBlockState(pos, ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK.getDefaultState());
            }
        }

        // ===== TORN WALLPAPER =====

        else if (current == ModBlocks.TORN_WALLPAPER_BLOCK) {
            if (random.nextInt(8) == 0) {
                world.setBlockState(pos, ModBlocks.STAINED_TORN_WALLPAPER_BLOCK.getDefaultState());
            }
        }

        else if (current == ModBlocks.STAINED_TORN_WALLPAPER_BLOCK) {
            if (random.nextInt(8) == 0) {
                world.setBlockState(pos, ModBlocks.WET_TORN_WALLPAPER_BLOCK.getDefaultState());
            }
        }

        else if (current == ModBlocks.WET_TORN_WALLPAPER_BLOCK) {
            if (random.nextInt(10) == 0) {
                world.setBlockState(pos, ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK.getDefaultState());
            }
        }

        else if (current == ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK) {
            if (random.nextInt(12) == 0) {
                world.setBlockState(pos, ModBlocks.MOLD_INFECTED_TORN_WALLPAPER_BLOCK.getDefaultState());
            }
        }
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (stack.isOf(Items.SHEARS)) {
            if (!world.isClient) {
                BlockState tornState = getTornVariant(state);

                if (tornState != null && tornState != state) {
                    world.setBlockState(pos, tornState, Block.NOTIFY_ALL);
                    stack.damage(1, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                    world.playSound(null, pos, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.BLOCKS, 1.0f, 1.0f);

                    // Drop 1 paper when shearing wallpaper
                    Block.dropStack(world, pos, new ItemStack(Items.PAPER, 1));
                }
            }
            return ItemActionResult.success(world.isClient);
        }

        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private BlockState getTornVariant(BlockState currentState) {
        Block currentBlock = currentState.getBlock();

        if (currentBlock == ModBlocks.WALLPAPER_BLOCK) {
            return ModBlocks.TORN_WALLPAPER_BLOCK.getDefaultState();
        }
        if (currentBlock == ModBlocks.STAINED_WALLPAPER_BLOCK) {
            return ModBlocks.STAINED_TORN_WALLPAPER_BLOCK.getDefaultState();
        }
        if (currentBlock == ModBlocks.WET_WALLPAPER_BLOCK) {
            return ModBlocks.WET_TORN_WALLPAPER_BLOCK.getDefaultState();
        }
        if (currentBlock == ModBlocks.MOLDY_WALLPAPER_BLOCK) {
            return ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK.getDefaultState();
        }
        if (currentBlock == ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK) {
            return ModBlocks.MOLD_INFECTED_TORN_WALLPAPER_BLOCK.getDefaultState();
        }
        if (currentBlock == ModBlocks.SPONGE_WALLPAPER_BLOCK) {
            return currentState;
        }
        if (currentBlock == ModBlocks.TORN_WALLPAPER_BLOCK ||
                currentBlock == ModBlocks.STAINED_TORN_WALLPAPER_BLOCK ||
                currentBlock == ModBlocks.WET_TORN_WALLPAPER_BLOCK ||
                currentBlock == ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK ||
                currentBlock == ModBlocks.MOLD_INFECTED_TORN_WALLPAPER_BLOCK) {
            return currentState;
        }

        return currentState;
    }

    /**
     * Check if there's water nearby (within 2 blocks).
     */
    private boolean isNearWater(World world, BlockPos pos) {
        for (BlockPos checkPos : BlockPos.iterate(
                pos.add(-2, -2, -2),
                pos.add(2, 2, 2))) {

            if (world.getFluidState(checkPos).isOf(Fluids.WATER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if there are bacteria blocks nearby (within 3 blocks).
     * Bacteria shrooms and vines accelerate wallpaper decay.
     */
    private boolean isNearBacteria(World world, BlockPos pos) {
        for (BlockPos checkPos : BlockPos.iterate(
                pos.add(-3, -3, -3),
                pos.add(3, 3, 3))) {

            BlockState state = world.getBlockState(checkPos);

            if (state.isOf(ModBlocks.BACTERIA_SHROOM_HORIZONTAL) ||
                    state.isOf(ModBlocks.BACTERIA_SHROOM_VERTICAL) ||
                    state.isOf(ModBlocks.BACTERIA_VINE)) {
                return true;
            }
        }
        return false;
    }
}
