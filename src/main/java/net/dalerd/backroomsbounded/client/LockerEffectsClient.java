package net.dalerd.backroomsbounded.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import net.minecraft.util.Identifier;

public class LockerEffectsClient {

    private static final Identifier OVERLAY =
            Identifier.of(
                    "backroomsbounded",
                    "textures/misc/locker_overlay.png"
            );

    public static boolean inLocker = false;

    public static void register() {

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {

            MinecraftClient client =
                    MinecraftClient.getInstance();

            // No player/world
            if (client.player == null || client.world == null) {
                return;
            }

            // Don't render in menus
            if (client.currentScreen != null) {
                return;
            }

            // Not inside locker
            if (!inLocker) {
                return;
            }

            renderOverlay(drawContext);
        });
    }

    private static void renderOverlay(
            DrawContext drawContext
    ) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        int width =
                client.getWindow().getScaledWidth();

        int height =
                client.getWindow().getScaledHeight();

        RenderSystem.enableBlend();

        // Overlay transparency
        RenderSystem.setShaderColor(
                1f,
                1f,
                1f,
                0.65f
        );

        drawContext.drawTexture(
                OVERLAY,

                0,
                0,

                0,
                0,

                width,
                height,

                width,
                height
        );

        // Reset render state
        RenderSystem.setShaderColor(
                1f,
                1f,
                1f,
                1f
        );

        RenderSystem.disableBlend();
    }
}
