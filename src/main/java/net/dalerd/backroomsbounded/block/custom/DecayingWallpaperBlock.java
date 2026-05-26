package net.dalerd.backroomsbounded.block.custom;

import net.dalerd.backroomsbounded.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Waterloggable;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.world.ServerWorld;
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
