package net.dalerd.backroomsbounded.event;

import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

public class BackroomsLootHandler {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // Only apply in backrooms dimension
            if (world.isClient) return ActionResult.PASS;
            if (world.getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) return ActionResult.PASS;

            // Check if the block is a chest or barrel
            var state = world.getBlockState(hitResult.getBlockPos());
            if (!(state.getBlock() instanceof ChestBlock) && !(state.getBlock() instanceof BarrelBlock)) {
                return ActionResult.PASS;
            }

            // Check if the block entity is a lootable container without a loot table
            var blockEntity = world.getBlockEntity(hitResult.getBlockPos());
            if (blockEntity instanceof LootableContainerBlockEntity container) {
                if (container.getLootTable() == null) {
                    // Set appropriate loot table
                    RegistryKey<net.minecraft.loot.LootTable> lootTableKey;
                    if (state.getBlock() instanceof ChestBlock) {
                        lootTableKey = RegistryKey.of(RegistryKeys.LOOT_TABLE,
                                Identifier.of("backroomsbounded", "blocks/backrooms_chest"));
                    } else {
                        lootTableKey = RegistryKey.of(RegistryKeys.LOOT_TABLE,
                                Identifier.of("backroomsbounded", "blocks/backrooms_barrel"));
                    }
                    container.setLootTable(lootTableKey, world.getRandom().nextLong());
                }
            }

            return ActionResult.PASS;
        });
    }
}
