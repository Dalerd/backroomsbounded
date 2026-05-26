package net.dalerd.backroomsbounded.block.entity;

import net.minecraft.block.BlockState;

import net.minecraft.block.entity.BlockEntity;

import net.minecraft.nbt.NbtCompound;

import net.minecraft.registry.RegistryWrapper;

import net.minecraft.util.math.BlockPos;

import net.minecraft.world.World;

public class WaterCoolerBlockEntity extends BlockEntity {

    private static final int MAX_SERVINGS = 8;

    // 20 minutes = 24000 ticks
    private static final int REFILL_TIME = 24000;

    private int servings = MAX_SERVINGS;

    private int refillTimer = 0;

    public WaterCoolerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_COOLER_BE, pos, state);
    }

    public boolean hasServings() {
        return servings > 0;
    }

    public void takeServing() {

        if (servings > 0) {

            servings--;

            markDirty();
        }
    }

    public static void tick(
            World world,
            BlockPos pos,
            BlockState state,
            WaterCoolerBlockEntity entity
    ) {

        if (world.isClient()) {
            return;
        }

        if (entity.servings < MAX_SERVINGS) {

            entity.refillTimer++;

            if (entity.refillTimer >= REFILL_TIME) {

                entity.servings++;

                entity.refillTimer = 0;

                entity.markDirty();
            }
        }
    }

    @Override
    protected void writeNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {

        super.writeNbt(nbt, registryLookup);

        nbt.putInt("servings", servings);
        nbt.putInt("refill_timer", refillTimer);
    }

    @Override
    protected void readNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {

        super.readNbt(nbt, registryLookup);

        servings = nbt.getInt("servings");

        refillTimer = nbt.getInt("refill_timer");
    }
}
