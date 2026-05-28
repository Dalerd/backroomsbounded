package net.dalerd.backroomsbounded.mixin;

import net.dalerd.backroomsbounded.event.RandomBackroomsTeleportHandler;

import net.minecraft.block.BlockState;
import net.minecraft.block.NetherPortalBlock;

import net.minecraft.entity.Entity;

import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.util.math.BlockPos;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {

    @Inject(
            method = "onEntityCollision",
            at = @At("HEAD")
    )
    private void backroomsbounded$portalChance(
            BlockState state,
            World world,
            BlockPos pos,
            Entity entity,
            CallbackInfo ci
    ) {

        if (world.isClient()) {
            return;
        }

        if (entity instanceof ServerPlayerEntity player) {

            RandomBackroomsTeleportHandler.tryPortalTeleport(
                    player
            );
        }
    }
}
