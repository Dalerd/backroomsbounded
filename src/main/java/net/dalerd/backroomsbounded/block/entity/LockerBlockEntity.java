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
        super(ModBlockEntities.LOCKER_BE, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, LockerBlockEntity be) {

        // Safety check
        if (!(state.getBlock() instanceof LockerBlock)) {
            return;
        }

        // Reset client locker effect
        if (world.isClient()) {
            LockerEffectsClient.inLocker = false;
        }

        // Always use LOWER HALF as base
        BlockPos basePos = state.get(LockerBlock.HALF).asString().equals("upper")
                ? pos.down()
                : pos;

        // Full locker interior
        Box inside = new Box(
                basePos.getX() + 0.1,
                basePos.getY() + 0.1,
                basePos.getZ() + 0.1,

                basePos.getX() + 0.9,
                basePos.getY() + 1.9,
                basePos.getZ() + 0.9
        );

        // Detect player inside
        for (PlayerEntity player : world.getEntitiesByClass(
                PlayerEntity.class,
                inside,
                p -> true
        )) {

            if (world.isClient()) {
                LockerEffectsClient.inLocker = true;
            }
        }

        // Client stops here
        if (world.isClient()) {
            return;
        }

        // Nearby hostile entities
        Box nearby = new Box(basePos).expand(1.5);

        for (Entity entity : world.getOtherEntities(null, nearby)) {

            if (entity instanceof WitherSkeletonEntity) {

                // Random damage chance
                if (world.random.nextInt(80) == 0) {

                    be.damage += 3;

                    world.syncWorldEvent(
                            2001,
                            basePos,
                            Block.getRawIdFromState(state)
                    );

                    // Break locker
                    if (be.damage >= 10) {

                        world.breakBlock(basePos, false);

                        BlockPos upper = basePos.up();

                        if (world.getBlockState(upper).getBlock() instanceof LockerBlock) {
                            world.breakBlock(upper, false);
                        }

                        break;
                    }
                }
            }
        }

        be.markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);

        nbt.putInt("damage", damage);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);

        damage = nbt.getInt("damage");
    }
}
