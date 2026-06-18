package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class CarpetSoundReducer {

    /**
     * Check if a player is walking on wool or carpet.
     * Call this from entity detection methods.
     * @return true if the player's footstep sounds should be reduced
     */
    public static boolean isOnQuietSurface(PlayerEntity player) {
        BlockPos footPos = player.getBlockPos();
        BlockState blockBelow = player.getWorld().getBlockState(footPos.down());

        // Check if standing on wool or carpet
        if (blockBelow.isOf(Blocks.WHITE_WOOL) || blockBelow.isOf(Blocks.ORANGE_WOOL) ||
                blockBelow.isOf(Blocks.MAGENTA_WOOL) || blockBelow.isOf(Blocks.LIGHT_BLUE_WOOL) ||
                blockBelow.isOf(Blocks.YELLOW_WOOL) || blockBelow.isOf(Blocks.LIME_WOOL) ||
                blockBelow.isOf(Blocks.PINK_WOOL) || blockBelow.isOf(Blocks.GRAY_WOOL) ||
                blockBelow.isOf(Blocks.LIGHT_GRAY_WOOL) || blockBelow.isOf(Blocks.CYAN_WOOL) ||
                blockBelow.isOf(Blocks.PURPLE_WOOL) || blockBelow.isOf(Blocks.BLUE_WOOL) ||
                blockBelow.isOf(Blocks.BROWN_WOOL) || blockBelow.isOf(Blocks.GREEN_WOOL) ||
                blockBelow.isOf(Blocks.RED_WOOL) || blockBelow.isOf(Blocks.BLACK_WOOL) ||
                blockBelow.isOf(Blocks.WHITE_CARPET) || blockBelow.isOf(Blocks.ORANGE_CARPET) ||
                blockBelow.isOf(Blocks.MAGENTA_CARPET) || blockBelow.isOf(Blocks.LIGHT_BLUE_CARPET) ||
                blockBelow.isOf(Blocks.YELLOW_CARPET) || blockBelow.isOf(Blocks.LIME_CARPET) ||
                blockBelow.isOf(Blocks.PINK_CARPET) || blockBelow.isOf(Blocks.GRAY_CARPET) ||
                blockBelow.isOf(Blocks.LIGHT_GRAY_CARPET) || blockBelow.isOf(Blocks.CYAN_CARPET) ||
                blockBelow.isOf(Blocks.PURPLE_CARPET) || blockBelow.isOf(Blocks.BLUE_CARPET) ||
                blockBelow.isOf(Blocks.BROWN_CARPET) || blockBelow.isOf(Blocks.GREEN_CARPET) ||
                blockBelow.isOf(Blocks.RED_CARPET) || blockBelow.isOf(Blocks.BLACK_CARPET) ||
                blockBelow.isOf(Blocks.MOSS_BLOCK) || blockBelow.isOf(Blocks.MOSS_CARPET)) {
            return true;
        }
        return false;
    }

    /**
     * Get the sound reduction multiplier for detection.
     * @return 0.5f if on quiet surface (half detection range), 1.0f otherwise
     */
    public static float getSoundMultiplier(PlayerEntity player) {
        if (!isInBackrooms(player)) return 1.0f;
        return isOnQuietSurface(player) ? 0.5f : 1.0f;
    }

    private static boolean isInBackrooms(PlayerEntity player) {
        return player.getWorld().getRegistryKey() == BackroomsDimension.BACKROOMS_LEVEL_KEY;
    }
}