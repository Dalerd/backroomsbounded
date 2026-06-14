package net.dalerd.backroomsbounded.block.custom;

import net.dalerd.backroomsbounded.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class DecayingWallpaperBlock extends Block {

    public DecayingWallpaperBlock(Settings settings) {
        super(settings.ticksRandomly());
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {

        if (!isNearWater(world, pos)) {
            return;
        }

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
        // Check if player is holding shears
        if (stack.isOf(Items.SHEARS)) {
            if (!world.isClient) {
                // Get the torn variant of this wallpaper
                BlockState tornState = getTornVariant(state);

                if (tornState != null && tornState != state) {
                    // Replace the block with its torn variant
                    world.setBlockState(pos, tornState, Block.NOTIFY_ALL);

                    // Damage the shears
                    stack.damage(1, player, hand == Hand.MAIN_HAND ? net.minecraft.entity.EquipmentSlot.MAINHAND : net.minecraft.entity.EquipmentSlot.OFFHAND);

                    // Play shears sound
                    world.playSound(null, pos, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.BLOCKS, 1.0f, 1.0f);
                }
            }
            return ItemActionResult.success(world.isClient);
        }

        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private BlockState getTornVariant(BlockState currentState) {
        Block currentBlock = currentState.getBlock();

        // Map each wallpaper to its torn variant
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
            return currentState; // Sponge stays sponge
        }
        // Already torn variants stay as they are
        if (currentBlock == ModBlocks.TORN_WALLPAPER_BLOCK ||
                currentBlock == ModBlocks.STAINED_TORN_WALLPAPER_BLOCK ||
                currentBlock == ModBlocks.WET_TORN_WALLPAPER_BLOCK ||
                currentBlock == ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK ||
                currentBlock == ModBlocks.MOLD_INFECTED_TORN_WALLPAPER_BLOCK) {
            return currentState; // Already torn, no change
        }

        return currentState; // No torn variant available
    }

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
}
