package net.dalerd.backroomsbounded.datagen;

import net.dalerd.backroomsbounded.block.ModBlocks;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;

import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {

    public ModBlockTagProvider(
            FabricDataOutput output,
            CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture
    ) {
        super(output, completableFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {

        // =========================
        // MINEABLE WITH AXE
        // =========================

        getOrCreateTagBuilder(BlockTags.AXE_MINEABLE)

                .add(ModBlocks.BACKBOARD_BLOCK)

                .add(ModBlocks.WALLPAPER_BLOCK)
                .add(ModBlocks.STAINED_WALLPAPER_BLOCK)
                .add(ModBlocks.WET_WALLPAPER_BLOCK)
                .add(ModBlocks.MOLDY_WALLPAPER_BLOCK)
                .add(ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK)

                .add(ModBlocks.TORN_WALLPAPER_BLOCK)
                .add(ModBlocks.STAINED_TORN_WALLPAPER_BLOCK)
                .add(ModBlocks.WET_TORN_WALLPAPER_BLOCK)
                .add(ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK)
                .add(ModBlocks.MOLD_INFECTED_TORN_WALLPAPER_BLOCK)

                .add(ModBlocks.SPONGE_WALLPAPER_BLOCK);

        // =========================
        // MINEABLE WITH PICKAXE
        // =========================

        getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE)

                .add(ModBlocks.LOCKER);

        // =========================
        // REQUIRES STONE TOOL
        // =========================

        getOrCreateTagBuilder(BlockTags.NEEDS_STONE_TOOL)

                .add(ModBlocks.BACKBOARD_BLOCK)

                .add(ModBlocks.WALLPAPER_BLOCK)
                .add(ModBlocks.STAINED_WALLPAPER_BLOCK)
                .add(ModBlocks.WET_WALLPAPER_BLOCK)
                .add(ModBlocks.MOLDY_WALLPAPER_BLOCK)
                .add(ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK)

                .add(ModBlocks.TORN_WALLPAPER_BLOCK)
                .add(ModBlocks.STAINED_TORN_WALLPAPER_BLOCK)
                .add(ModBlocks.WET_TORN_WALLPAPER_BLOCK)
                .add(ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK)
                .add(ModBlocks.MOLD_INFECTED_TORN_WALLPAPER_BLOCK)

                .add(ModBlocks.SPONGE_WALLPAPER_BLOCK)

                .add(ModBlocks.LOCKER);
    }
}
