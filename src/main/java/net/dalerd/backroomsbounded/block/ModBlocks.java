package net.dalerd.backroomsbounded.block;

import net.dalerd.backroomsbounded.BackroomsBounded;
import net.dalerd.backroomsbounded.block.custom.LockerBlock;
import net.dalerd.backroomsbounded.block.custom.WaterCoolerBlock;
import net.dalerd.backroomsbounded.block.custom.DecayingWallpaperBlock;
import net.dalerd.backroomsbounded.block.custom.BacteriaShroomHorizontalBlock;
import net.dalerd.backroomsbounded.block.custom.BacteriaShroomVerticalBlock;
import net.dalerd.backroomsbounded.block.custom.BacteriaVineBlock;
import net.dalerd.backroomsbounded.block.custom.BacteriaShroomItem;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {

    // =========================
    // LOCKER
    // =========================
    public static final Block LOCKER = registerBlock(
            "locker",
            new LockerBlock(
                    AbstractBlock.Settings.create()
                            .strength(2.5f, 3.0f)
                            .requiresTool()
                            .sounds(BlockSoundGroup.METAL)
            )
    );

    // =========================
    // WATER COOLER
    // =========================
    public static final Block WATER_COOLER = registerBlock(
            "water_cooler",
            new WaterCoolerBlock(
                    AbstractBlock.Settings.create()
                            .strength(2.0f)
                            .requiresTool()
                            .sounds(BlockSoundGroup.METAL)
                            .nonOpaque()
            )
    );

    // =========================
    // GLITCH BLOCK
    // =========================
    public static final Block GLITCH_BLOCK = registerBlock(
            "glitch_block",
            new GlitchBlock(
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.BLACK)
                            .strength(-1.0f)
                            .noCollision()
                            .nonOpaque()
                            .luminance(state -> 7)
            )
    );

    // =========================
    // BACKBOARD BLOCK
    // =========================
    public static final Block BACKBOARD_BLOCK = registerBlock(
            "backboard_block",
            new Block(
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.PALE_YELLOW)
                            .strength(2.0f)
                            .sounds(BlockSoundGroup.WOOD)
            )
    );

    // =========================
    // WALLPAPER BLOCKS
    // =========================
    public static final Block WALLPAPER_BLOCK = registerBlock(
            "wallpaper_block",
            new DecayingWallpaperBlock(
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.PALE_YELLOW)
                            .strength(1.5f)
                            .sounds(BlockSoundGroup.WOOL)
            )
    );

    public static final Block STAINED_WALLPAPER_BLOCK = registerBlock(
            "stained_wallpaper_block",
            new DecayingWallpaperBlock(
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.PALE_YELLOW)
                            .strength(1.5f)
                            .sounds(BlockSoundGroup.WOOL)
            )
    );

    public static final Block WET_WALLPAPER_BLOCK = registerBlock(
            "wet_wallpaper_block",
            new DecayingWallpaperBlock(
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.PALE_YELLOW)
                            .strength(0.4f)
                            .sounds(BlockSoundGroup.WOOL)
            )
    );

    public static final Block MOLDY_WALLPAPER_BLOCK = registerBlock(
            "moldy_wallpaper_block",
            new DecayingWallpaperBlock(
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.DARK_GREEN)
                            .strength(0.2f)
                            .sounds(BlockSoundGroup.WOOL)
            )
    );

    public static final Block MOLD_INFECTED_WALLPAPER_BLOCK = registerBlock(
            "mold_infected_wallpaper_block",
            new DecayingWallpaperBlock(
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.GREEN)
                            .strength(0.15f)
                            .sounds(BlockSoundGroup.WOOL)
            )
    );

    // =========================
    // TORN WALLPAPER BLOCKS
    // =========================
    public static final Block TORN_WALLPAPER_BLOCK = registerBlock(
            "torn_wallpaper_block",
            new DecayingWallpaperBlock(
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.PALE_YELLOW)
                            .strength(1f)
                            .sounds(BlockSoundGroup.WOOL)
            )
    );

    public static final Block STAINED_TORN_WALLPAPER_BLOCK = registerBlock(
            "stained_torn_wallpaper_block",
            new DecayingWallpaperBlock(
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.PALE_YELLOW)
                            .strength(0.5f)
                            .sounds(BlockSoundGroup.WOOL)
            )
    );

    public static final Block WET_TORN_WALLPAPER_BLOCK = registerBlock(
            "wet_torn_wallpaper_block",
            new DecayingWallpaperBlock(
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.PALE_YELLOW)
                            .strength(0.4f)
                            .sounds(BlockSoundGroup.WOOL)
            )
    );

    public static final Block MOLDY_TORN_WALLPAPER_BLOCK = registerBlock(
            "moldy_torn_wallpaper_block",
            new DecayingWallpaperBlock(
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.DARK_GREEN)
                            .strength(0.2f)
                            .sounds(BlockSoundGroup.WOOL)
            )
    );

    public static final Block MOLD_INFECTED_TORN_WALLPAPER_BLOCK = registerBlock(
            "mold_infected_torn_wallpaper_block",
            new DecayingWallpaperBlock(
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.GREEN)
                            .strength(0.15f)
                            .sounds(BlockSoundGroup.WOOL)
            )
    );

    // =========================
    // SPONGE WALLPAPER BLOCK
    // =========================
    public static final Block SPONGE_WALLPAPER_BLOCK = registerBlock(
            "sponge_wallpaper_block",
            new Block(
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.PALE_YELLOW)
                            .strength(1.0f)
                            .sounds(BlockSoundGroup.WOOL)
            )
    );

    // =========================
    // BACTERIA ECOSYSTEM
    // =========================

    // Horizontal - placed on top of blocks, has a custom item that auto-switches
    public static final Block BACTERIA_SHROOM_HORIZONTAL = registerBlockWithCustomItem(
            "bacteria_shroom_horizontal",
            new BacteriaShroomHorizontalBlock(
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.DARK_GREEN)
                            .strength(0.5f)
                            .sounds(BlockSoundGroup.FUNGUS)
                            .nonOpaque()
            )
    );

    // Vertical - placed on walls, no item (placed automatically by the horizontal item)
    public static final Block BACTERIA_SHROOM_VERTICAL = registerBlockNoItem(
            "bacteria_shroom_vertical",
            new BacteriaShroomVerticalBlock(
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.DARK_GREEN)
                            .strength(0.5f)
                            .sounds(BlockSoundGroup.FUNGUS)
                            .nonOpaque()
            )
    );

    // Vine - acts like glow lichen
    public static final Block BACTERIA_VINE = registerBlock(
            "bacteria_vine",
            new BacteriaVineBlock(
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.DARK_GREEN)
                            .strength(0.2f)
                            .sounds(BlockSoundGroup.FUNGUS)
                            .nonOpaque()
                            .luminance(state -> 7)
                            .noCollision()
            )
    );

    // =========================
    // REGISTER METHODS
    // =========================

    // Standard block + BlockItem
    private static <T extends Block> T registerBlock(String name, T block) {
        Registry.register(
                Registries.ITEM,
                Identifier.of(BackroomsBounded.MOD_ID, name),
                new BlockItem(block, new Item.Settings())
        );

        return Registry.register(
                Registries.BLOCK,
                Identifier.of(BackroomsBounded.MOD_ID, name),
                block
        );
    }

    // Block with custom BacteriaShroomItem (auto-switches horizontal/vertical on placement)
    private static <T extends Block> T registerBlockWithCustomItem(String name, T block) {
        Registry.register(
                Registries.ITEM,
                Identifier.of(BackroomsBounded.MOD_ID, name),
                new BacteriaShroomItem(block, new Item.Settings())
        );

        return Registry.register(
                Registries.BLOCK,
                Identifier.of(BackroomsBounded.MOD_ID, name),
                block
        );
    }

    // Block only, no item (vertical shroom is placed by the horizontal item)
    private static <T extends Block> T registerBlockNoItem(String name, T block) {
        return Registry.register(
                Registries.BLOCK,
                Identifier.of(BackroomsBounded.MOD_ID, name),
                block
        );
    }

    // =========================
    // INIT
    // =========================
    public static void registerModBlocks() {
        BackroomsBounded.LOGGER.info("Registering Mod Blocks");
    }
}