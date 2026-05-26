package net.dalerd.backroomsbounded.world.gen;

import net.dalerd.backroomsbounded.BackroomsBounded;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class BackroomsDimension {

    public static final RegistryKey<World> BACKROOMS_LEVEL_KEY =
            RegistryKey.of(
                    RegistryKeys.WORLD,
                    Identifier.of(BackroomsBounded.MOD_ID, "backrooms")
            );
}
