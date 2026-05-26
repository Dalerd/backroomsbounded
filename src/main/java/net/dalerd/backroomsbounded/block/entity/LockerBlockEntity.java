package net.dalerd.backroomsbounded.block.entity;

import net.dalerd.backroomsbounded.block.custom.LockerBlock;
import net.dalerd.backroomsbounded.client.LockerEffectsClient;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

import net.minecraft.block.entity.BlockEntity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;

import net.minecraft.nbt.NbtCompound;

import net.minecraft.registry.RegistryWrapper;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import net.minecraft.world.World;

public class LockerBlockEntity extends BlockEntity {

    private int damage = 0;

    public LockerBlockEntity(BlockPos pos, BlockState state) {

        super(
                ModBlockEntities.LOCKER_BE,
                pos,
                state
        );
    }

    public static void tick(
            World world,
            BlockPos pos,
            BlockState state,
            LockerBlockEntity be
    ) {

        // Safety check
        if (!(state.getBlock() instanceof LockerBlock)) {
            return;
        }

        // Only LOWER HALF runs logic
        if (state.get(LockerBlock.HALF).asString().equals("upper")) {
            return;
        }

        // Reset client effect
        if (world.isClient()) {
            LockerEffectsClient.inLocker = false;
        }

        // Interior box
        Box inside = new Box(
                pos.getX() + 0.15,
                pos.getY() + 0.05,
                pos.getZ() + 0.15,

                pos.getX() + 0.85,
                pos.getY() + 1.95,
                pos.getZ() + 0.85
        );

        // Detect player inside
        for (PlayerEntity player : world.getEntitiesByClass(
                PlayerEntity.class,
                inside,
                player -> true
        )) {

            if (world.isClient()) {
                LockerEffectsClient.inLocker = true;
            }
        }

        // Client ends here
        if (world.isClient()) {
            return;
        }

        // Nearby hostile entities
        Box nearby = new Box(pos).expand(1.5);

        for (Entity entity : world.getOtherEntities(
                null,
                nearby
        )) {

            if (entity instanceof WitherSkeletonEntity) {

                // Random damage chance
                if (world.random.nextInt(80) == 0) {

                    be.damage += 3;

                    world.syncWorldEvent(
                            2001,
                            pos,
                            Block.getRawIdFromState(state)
                    );

                    // Break locker
                    if (be.damage >= 10) {

                        // Break lower
                        world.breakBlock(
                                pos,
                                false
                        );

                        // Break upper
                        BlockPos upperPos = pos.up();

                        if (world.getBlockState(upperPos).getBlock() instanceof LockerBlock) {

                            world.breakBlock(
                                    upperPos,
                                    false
                            );
                        }

                        break;
                    }
                }
            }
        }

        be.markDirty();
    }

    @Override
    protected void writeNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup lookup
    ) {

        super.writeNbt(
                nbt,
                lookup
        );

        nbt.putInt(
                "damage",
                damage
        );
    }

    @Override
    protected void readNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup lookup
    ) {

        super.readNbt(
                nbt,
                lookup
        );

        damage = nbt.getInt("damage");
    }
}
