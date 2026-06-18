package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

public class BackroomsLootHandler {

    private static final Set<BlockPos> LOOTED_CONTAINERS = new HashSet<>();

    public static void register() {

        // =========================================
        // INTERCEPT BARREL PLACEMENT - TURN INTO CHEST
        // =========================================
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) return ActionResult.PASS;

            // Check if player is holding a barrel item
            var heldStack = player.getStackInHand(hand);
            if (heldStack.isOf(Items.BARREL)) {
                // Calculate where the barrel would be placed
                BlockPos placePos = hitResult.getBlockPos().offset(hitResult.getSide());

                // Replace the barrel item action: place a chest instead
                // Consume one barrel from the stack
                if (!player.isCreative()) {
                    heldStack.decrement(1);
                }

                // Place a chest at the target position
                var facing = player.getHorizontalFacing().getOpposite();
                world.setBlockState(placePos, Blocks.CHEST.getDefaultState()
                        .with(ChestBlock.FACING, facing)
                        .with(Properties.WATERLOGGED, false));

                // Mark as player-placed so it doesn't get loot
                LOOTED_CONTAINERS.add(placePos.toImmutable());

                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });

        // =========================================
        // GIVE LOOT TO GENERATED BARRELS
        // =========================================
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) return ActionResult.PASS;

            var state = world.getBlockState(hitResult.getBlockPos());
            if (!(state.getBlock() instanceof BarrelBlock)) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos().toImmutable();

            if (LOOTED_CONTAINERS.contains(pos)) return ActionResult.PASS;

            var blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof LootableContainerBlockEntity container) {
                if (container.getLootTable() == null) {
                    container.setLootTable(
                            RegistryKey.of(RegistryKeys.LOOT_TABLE,
                                    Identifier.of("backroomsbounded", "blocks/backrooms_barrel")),
                            world.getRandom().nextLong()
                    );
                    LOOTED_CONTAINERS.add(pos);
                    world.updateListeners(pos, state, state, 3);
                }
            }

            return ActionResult.PASS;
        });
    }
}
