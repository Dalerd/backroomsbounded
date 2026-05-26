package net.dalerd.backroomsbounded;

import net.dalerd.backroomsbounded.block.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemGroups;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class BackroomsBoundedClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LOCKER, RenderLayer.getCutout());

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {

            entries.add(ModBlocks.BACKBOARD_BLOCK);
            entries.add(ModBlocks.WALLPAPER_BLOCK);

        });

        BackroomsBounded.LOGGER.info("BackroomsBounded client initialized.");
    }
}
