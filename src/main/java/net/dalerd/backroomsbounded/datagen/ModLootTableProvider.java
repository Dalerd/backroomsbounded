package net.dalerd.backroomsbounded.datagen;

import net.dalerd.backroomsbounded.block.ModBlocks;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;

import net.minecraft.block.Block;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends FabricBlockLootTableProvider {

    public ModLootTableProvider(
            FabricDataOutput dataOutput,
            CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup
    ) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {

        // =========================
        // NORMAL SELF DROPS
        // =========================

        addDrop(ModBlocks.BACKBOARD_BLOCK);
        addDrop(ModBlocks.WALLPAPER_BLOCK);
        addDrop(ModBlocks.TORN_WALLPAPER_BLOCK);
        addDrop(ModBlocks.SPONGE_WALLPAPER_BLOCK);

        // =========================
        // LOCKER
        // Drops 1-4 iron ingots
        // =========================

// =========================
// LOCKER
// Drops 1-4 iron ingots
// =========================

        addDrop(
                ModBlocks.LOCKER,
                LootTable.builder().pool(
                        addSurvivesExplosionCondition(
                                ModBlocks.LOCKER,
                                net.minecraft.loot.LootPool.builder()
                                        .rolls(net.minecraft.loot.provider.number.ConstantLootNumberProvider.create(1))
                                        .with(
                                                ItemEntry.builder(Items.IRON_INGOT)
                                                        .apply(SetCountLootFunction.builder(
                                                                UniformLootNumberProvider.create(1.0f, 4.0f)
                                                        ))
                                        )
                        )
                )
        );

        // =========================
        // STAINED / WET
        // Silk Touch = block
        // Otherwise = planks
        // =========================

        addDrop(
                ModBlocks.STAINED_WALLPAPER_BLOCK,
                silkOrMaterials(ModBlocks.STAINED_WALLPAPER_BLOCK, false)
        );

        addDrop(
                ModBlocks.WET_WALLPAPER_BLOCK,
                silkOrMaterials(ModBlocks.WET_WALLPAPER_BLOCK, false)
        );

        addDrop(
                ModBlocks.STAINED_TORN_WALLPAPER_BLOCK,
                silkOrMaterials(ModBlocks.STAINED_TORN_WALLPAPER_BLOCK, false)
        );

        addDrop(
                ModBlocks.WET_TORN_WALLPAPER_BLOCK,
                silkOrMaterials(ModBlocks.WET_TORN_WALLPAPER_BLOCK, false)
        );

        // =========================
        // MOLDY / INFECTED
        // Silk Touch = block
        // Otherwise = sticks
        // =========================

        addDrop(
                ModBlocks.MOLDY_WALLPAPER_BLOCK,
                silkOrMaterials(ModBlocks.MOLDY_WALLPAPER_BLOCK, true)
        );

        addDrop(
                ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK,
                silkOrMaterials(ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK, true)
        );

        addDrop(
                ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK,
                silkOrMaterials(ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK, true)
        );

        addDrop(
                ModBlocks.MOLD_INFECTED_TORN_WALLPAPER_BLOCK,
                silkOrMaterials(ModBlocks.MOLD_INFECTED_TORN_WALLPAPER_BLOCK, true)
        );
    }

    // =========================
    // CUSTOM LOOT
    // =========================

    private LootTable.Builder silkOrMaterials(Block block, boolean sticksOnly) {

        if (sticksOnly) {

            return dropsWithSilkTouch(
                    block,
                    this.applyExplosionDecay(
                            block,
                            ItemEntry.builder(Items.STICK)
                                    .apply(SetCountLootFunction.builder(
                                            UniformLootNumberProvider.create(0.0f, 4.0f)
                                    ))
                    )
            );

        } else {

            return dropsWithSilkTouch(
                    block,
                    this.applyExplosionDecay(
                            block,
                            ItemEntry.builder(Items.OAK_PLANKS)
                                    .apply(SetCountLootFunction.builder(
                                            UniformLootNumberProvider.create(0.0f, 3.0f)
                                    ))
                    )
            );
        }
    }
}
