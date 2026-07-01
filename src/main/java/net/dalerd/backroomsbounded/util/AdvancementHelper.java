package net.dalerd.backroomsbounded.util;

import net.dalerd.backroomsbounded.BackroomsBounded;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class AdvancementHelper {

    public static void grant(ServerPlayerEntity player, String advancementName) {

        AdvancementEntry advancement =
                player.getServer()
                        .getAdvancementLoader()
                        .get(Identifier.of(BackroomsBounded.MOD_ID, advancementName));

        if (advancement == null)
            return;

        AdvancementProgress progress =
                player.getAdvancementTracker().getProgress(advancement);

        for (String criterion : progress.getUnobtainedCriteria()) {
            player.getAdvancementTracker().grantCriterion(advancement, criterion);
        }
    }
}