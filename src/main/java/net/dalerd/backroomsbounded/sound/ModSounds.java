package net.dalerd.backroomsbounded.sound;

import net.dalerd.backroomsbounded.BackroomsBounded;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    public static final SoundEvent BACTERIUM_ROAR = register("entity.bacterium.roar");
    public static final SoundEvent AMBIENT_LOOP = register("ambient.backrooms.loop");
    public static final SoundEvent AMBIENT_MOOD = register("ambient.backrooms.mood");
    public static final SoundEvent AMBIENT_HEARTBEAT = register("ambient.backrooms.heartbeat");
    public static final SoundEvent PRINTER_PRINTING = register("block.printer.printing");

    private static SoundEvent register(String id) {
        Identifier soundId = Identifier.of(BackroomsBounded.MOD_ID, id);
        return Registry.register(Registries.SOUND_EVENT, soundId, SoundEvent.of(soundId));
    }

    public static void registerSounds() {
        BackroomsBounded.LOGGER.info("Registering Mod Sounds");
    }
}