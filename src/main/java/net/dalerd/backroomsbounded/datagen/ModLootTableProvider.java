package net.dalerd.backroomsbounded.datagen;

import net.dalerd.backroomsbounded.block.ModBlocks;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.MatchToolLootCondition;
import net.minecraft.loot.entry.AlternativeEntry;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.predicate.item.EnchantmentPredicate;
import net.minecraft.predicate.item.ItemPredicate;
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
        // NORMAL SELF DROPS (mineable by hand)
        // =========================

        addDrop(ModBlocks.BACKBOARD_BLOCK);
        addDrop(ModBlocks.WALLPAPER_BLOCK);
        addDrop(ModBlocks.TORN_WALLPAPER_BLOCK);
        addDrop(ModBlocks.SPONGE_WALLPAPER_BLOCK);

        // =========================
        // LOCKER
        // Drops 1-6 iron ingots (no silk touch needed)
        // =========================

        addDrop(ModBlocks.LOCKER,
                LootTable.builder()
                        .pool(LootPool.builder()
                                .rolls(UniformLootNumberProvider.create(1.0f, 6.0f))
                                .with(ItemEntry.builder(Items.IRON_INGOT))
                        )
        );

        // =========================
        // STAINED / WET WALLPAPER
        // Drops 0-3 oak planks
        // =========================

        addDrop(ModBlocks.STAINED_WALLPAPER_BLOCK,
                LootTable.builder()
                        .pool(LootPool.builder()
                                .rolls(UniformLootNumberProvider.create(0.0f, 3.0f))
                                .with(ItemEntry.builder(Items.OAK_PLANKS))
                        )
        );

        addDrop(ModBlocks.WET_WALLPAPER_BLOCK,
                LootTable.builder()
                        .pool(LootPool.builder()
                                .rolls(UniformLootNumberProvider.create(0.0f, 3.0f))
                                .with(ItemEntry.builder(Items.OAK_PLANKS))
                        )
        );

        addDrop(ModBlocks.STAINED_TORN_WALLPAPER_BLOCK,
                LootTable.builder()
                        .pool(LootPool.builder()
                                .rolls(UniformLootNumberProvider.create(0.0f, 3.0f))
                                .with(ItemEntry.builder(Items.OAK_PLANKS))
                        )
        );

        addDrop(ModBlocks.WET_TORN_WALLPAPER_BLOCK,
                LootTable.builder()
                        .pool(LootPool.builder()
                                .rolls(UniformLootNumberProvider.create(0.0f, 3.0f))
                                .with(ItemEntry.builder(Items.OAK_PLANKS))
                        )
        );

        // =========================
        // MOLDY / INFECTED WALLPAPER
        // Drops 0-4 sticks
        // =========================

        addDrop(ModBlocks.MOLDY_WALLPAPER_BLOCK,
                LootTable.builder()
                        .pool(LootPool.builder()
                                .rolls(UniformLootNumberProvider.create(0.0f, 4.0f))
                                .with(ItemEntry.builder(Items.STICK))
                        )
        );

        addDrop(ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK,
                LootTable.builder()
                        .pool(LootPool.builder()
                                .rolls(UniformLootNumberProvider.create(0.0f, 4.0f))
                                .with(ItemEntry.builder(Items.STICK))
                        )
        );

        addDrop(ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK,
                LootTable.builder()
                        .pool(LootPool.builder()
                                .rolls(UniformLootNumberProvider.create(0.0f, 4.0f))
                                .with(ItemEntry.builder(Items.STICK))
                        )
        );

        addDrop(ModBlocks.MOLD_INFECTED_TORN_WALLPAPER_BLOCK,
                LootTable.builder()
                        .pool(LootPool.builder()
                                .rolls(UniformLootNumberProvider.create(0.0f, 4.0f))
                                .with(ItemEntry.builder(Items.STICK))
                        )
        );
    }
}
