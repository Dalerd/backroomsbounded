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

            if (!inLocker) {
                return;
            }

            renderOverlay(drawContext);
        });
    }

    private static void renderOverlay(DrawContext drawContext) {

        MinecraftClient client = MinecraftClient.getInstance();

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // ENABLE TRANSPARENCY
        RenderSystem.enableBlend();

        // Alpha multiplier
        RenderSystem.setShaderColor(1f, 1f, 1f, 0.65f);

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

        // RESET
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        RenderSystem.disableBlend();
    }
}
