package net.dalerd.backroomsbounded.mixin;

import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugHud.class)
public class DisableDebugMixin {

    @Inject(method = "getLeftText", at = @At("RETURN"), cancellable = true)
    private void disableDebugLeft(CallbackInfoReturnable<List<String>> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.world != null) {
            if (client.world.getRegistryKey() == BackroomsDimension.BACKROOMS_LEVEL_KEY) {
                List<String> text = cir.getReturnValue();
                text.clear();
                text.add("[Debug disabled in the backrooms]");
            }
        }
    }

    @Inject(method = "getRightText", at = @At("RETURN"), cancellable = true)
    private void disableDebugRight(CallbackInfoReturnable<List<String>> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.world != null) {
            if (client.world.getRegistryKey() == BackroomsDimension.BACKROOMS_LEVEL_KEY) {
                List<String> text = cir.getReturnValue();
                text.clear();
            }
        }
    }
}