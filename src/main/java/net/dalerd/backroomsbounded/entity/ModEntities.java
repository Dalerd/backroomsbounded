package net.dalerd.backroomsbounded.entity;

import net.dalerd.backroomsbounded.BackroomsBounded;
import net.dalerd.backroomsbounded.entity.bacterium.BacteriumEntity;
import net.dalerd.backroomsbounded.entity.mimic.MimicEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<BacteriumEntity> BACTERIUM = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(BackroomsBounded.MOD_ID, "bacterium"),
            EntityType.Builder.create(BacteriumEntity::new, SpawnGroup.MISC)
                    .dimensions(1.0f, 3.0f)
                    .maxTrackingRange(128)
                    .trackingTickInterval(1)
                    .build("bacterium")
    );

    public static final EntityType<MimicEntity> MIMIC = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(BackroomsBounded.MOD_ID, "mimic"),
            EntityType.Builder.create(MimicEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6f, 1.8f)
                    .maxTrackingRange(80)
                    .trackingTickInterval(1)
                    .build("mimic")
    );

    public static void registerEntities() {
        FabricDefaultAttributeRegistry.register(BACTERIUM, BacteriumEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MIMIC, MimicEntity.createAttributes());
        BackroomsBounded.LOGGER.info("Registering Mod Entities");
    }
}