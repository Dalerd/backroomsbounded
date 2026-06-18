package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.entity.bacterium.BacteriumEntity;
import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;

import java.util.List;

public class BacteriumLearningHandler {

    private static final int LEARNING_RADIUS = 50; // Blocks

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) return ActionResult.PASS;

            var state = world.getBlockState(hitResult.getBlockPos());

            // Check if player opened a door
            if (state.getBlock() instanceof DoorBlock) {
                notifyNearbyBacterium(player, "door");
            }

            // Check if player opened a locker
            if (state.getBlock().getTranslationKey().contains("locker")) {
                notifyNearbyBacterium(player, "locker");
            }

            return ActionResult.PASS;
        });

        // Also detect block placement
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient) return;
            if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) return;
            notifyNearbyBacterium(player, "block");
        });
    }

    private static void notifyNearbyBacterium(PlayerEntity player, String action) {
        // Find all bacteria within learning radius
        Box searchBox = new Box(player.getBlockPos()).expand(LEARNING_RADIUS);
        List<BacteriumEntity> bacteria = player.getWorld().getEntitiesByClass(
                BacteriumEntity.class, searchBox, bacterium -> true);

        for (BacteriumEntity bacterium : bacteria) {
            switch (action) {
                case "door" -> bacterium.playerOpenedDoor();
                case "locker" -> bacterium.playerOpenedLocker();
                case "block" -> bacterium.playerPlacedBlock();
            }
        }
    }
}