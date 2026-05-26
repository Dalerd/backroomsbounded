package net.dalerd.backroomsbounded.item;

import net.dalerd.backroomsbounded.BackroomsBounded;

import net.dalerd.backroomsbounded.item.custom.AlmondWaterItem;

import net.minecraft.component.type.FoodComponent;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item ALMOND_WATER = registerItem(
            "almond_water",
            new AlmondWaterItem(
                    new Item.Settings()

                            .food(
                                    new FoodComponent.Builder()
                                            .nutrition(2)
                                            .saturationModifier(0.6f)
                                            .alwaysEdible()
                                            .build()
                            )

                            .recipeRemainder(Items.GLASS_BOTTLE)

                            .maxCount(16)
            )
    );

    private static Item registerItem(String name, Item item) {

        return Registry.register(
                Registries.ITEM,
                Identifier.of(BackroomsBounded.MOD_ID, name),
                item
        );
    }

    public static void registerModItems() {

        BackroomsBounded.LOGGER.info("Registering Mod Items");
    }
}
