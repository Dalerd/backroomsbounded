package net.dalerd.backroomsbounded.block.entity;

import net.dalerd.backroomsbounded.BackroomsBounded;
import net.dalerd.backroomsbounded.block.ModBlocks;

import net.minecraft.block.entity.BlockEntityType;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static final BlockEntityType<WaterCoolerBlockEntity>
            WATER_COOLER_BE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(BackroomsBounded.MOD_ID, "water_cooler_be"),
            BlockEntityType.Builder.create(
                    WaterCoolerBlockEntity::new,
                    ModBlocks.WATER_COOLER
            ).build(null)
    );

    public static final BlockEntityType<LockerBlockEntity>
            LOCKER_BE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(BackroomsBounded.MOD_ID, "locker_be"),
            BlockEntityType.Builder.create(
                    LockerBlockEntity::new,
                    ModBlocks.LOCKER
            ).build(null)
    );

    public static void registerBlockEntities() {
        BackroomsBounded.LOGGER.info("Registering Block Entities");
    }

}
