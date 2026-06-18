package net.dalerd.backroomsbounded.item;

import net.dalerd.backroomsbounded.BackroomsBounded;
import net.dalerd.backroomsbounded.block.ModBlocks;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {

    public static final ItemGroup BACKROOMS_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            Identifier.of(BackroomsBounded.MOD_ID, "backrooms"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModBlocks.WALLPAPER_BLOCK))
                    .displayName(Text.translatable("itemGroup.backroomsbounded.backrooms"))
                    .entries((displayContext, entries) -> {

                        entries.add(ModBlocks.BACKBOARD_BLOCK);

                        entries.add(ModBlocks.WALLPAPER_BLOCK);
                        entries.add(ModBlocks.STAINED_WALLPAPER_BLOCK);
                        entries.add(ModBlocks.WET_WALLPAPER_BLOCK);
                        entries.add(ModBlocks.MOLDY_WALLPAPER_BLOCK);
                        entries.add(ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK);

                        entries.add(ModBlocks.TORN_WALLPAPER_BLOCK);
                        entries.add(ModBlocks.STAINED_TORN_WALLPAPER_BLOCK);
                        entries.add(ModBlocks.WET_TORN_WALLPAPER_BLOCK);
                        entries.add(ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK);
                        entries.add(ModBlocks.MOLD_INFECTED_TORN_WALLPAPER_BLOCK);

                        entries.add(ModBlocks.SPONGE_WALLPAPER_BLOCK);

                        entries.add(ModItems.ALMOND_WATER);
                        entries.add(ModBlocks.WATER_COOLER);
                        entries.add(ModBlocks.LOCKER);
                        entries.add(ModBlocks.BACTERIA_SHROOM_HORIZONTAL);
                        entries.add(ModBlocks.BACTERIA_VINE);
                    })
                    .build()
    );

    public static void registerItemGroups() {

        BackroomsBounded.LOGGER.info("Registering Item Groups");
    }
}
