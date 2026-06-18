package net.dalerd.backroomsbounded;

import net.dalerd.backroomsbounded.block.ModBlocks;
import net.dalerd.backroomsbounded.client.LockerEffectsClient;
import net.dalerd.backroomsbounded.entity.ModEntities;
import net.dalerd.backroomsbounded.entity.bacterium.BacteriumRenderer;
import net.dalerd.backroomsbounded.entity.mimic.MimicRenderer;
import net.dalerd.backroomsbounded.sanity.SanityClientEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemGroups;

public class BackroomsBoundedClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.BACTERIUM, BacteriumRenderer::new);
        EntityRendererRegistry.register(ModEntities.MIMIC, MimicRenderer::new);
        LockerEffectsClient.register();
        SanityClientEvents.register();

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LOCKER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BACTERIA_SHROOM_HORIZONTAL, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BACTERIA_SHROOM_VERTICAL, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BACTERIA_VINE, RenderLayer.getCutout());

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.add(ModBlocks.BACKBOARD_BLOCK);
            entries.add(ModBlocks.WALLPAPER_BLOCK);
        });

        // Red static overlay when grabbed (darkness + nausea = grabbed by bacterium)
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null &&
                    client.player.hasStatusEffect(StatusEffects.DARKNESS) &&
                    client.player.hasStatusEffect(StatusEffects.NAUSEA)) {
                int width = client.getWindow().getScaledWidth();
                int height = client.getWindow().getScaledHeight();
                // Red transparent static overlay - 25% opacity red
                drawContext.fill(0, 0, width, height, 0x40FF0000);
            }
        });

        BackroomsBounded.LOGGER.info("BackroomsBounded client initialized.");
    }
}