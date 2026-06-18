package net.dalerd.backroomsbounded.entity.bacterium;

import net.minecraft.nbt.NbtCompound;

public class BacteriumData {
    public int doorsOpened = 0;
    public int lockersOpened = 0;
    public int blocksPlaced = 0;
    public int learningLevel = 0;

    public void fromNbt(NbtCompound nbt) {
        doorsOpened = nbt.getInt("DoorsOpened");
        lockersOpened = nbt.getInt("LockersOpened");
        blocksPlaced = nbt.getInt("BlocksPlaced");
        learningLevel = nbt.getInt("LearningLevel");
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("DoorsOpened", doorsOpened);
        nbt.putInt("LockersOpened", lockersOpened);
        nbt.putInt("BlocksPlaced", blocksPlaced);
        nbt.putInt("LearningLevel", learningLevel);
        return nbt;
    }
}
