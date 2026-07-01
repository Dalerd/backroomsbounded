package net.dalerd.backroomsbounded.advancement;

import net.dalerd.backroomsbounded.BackroomsBounded;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ModAdvancements {

    // Advancement IDs
    public static final Identifier ENTER_OFFICE = Identifier.of(BackroomsBounded.MOD_ID, "enter_office");
    public static final Identifier TRIGGER_BACTERIA_SHROOM = Identifier.of(BackroomsBounded.MOD_ID, "trigger_bacteria_shroom");
    public static final Identifier SURVIVE_BACTERIUM_GRAB = Identifier.of(BackroomsBounded.MOD_ID, "survive_bacterium_grab");
    public static final Identifier PANIC_ATTACK = Identifier.of(BackroomsBounded.MOD_ID, "panic_attack");
    public static final Identifier DEEP_OFFICE_EXPLORER = Identifier.of(BackroomsBounded.MOD_ID, "deep_office_explorer");
    public static final Identifier FAILED_ESCAPE = Identifier.of(BackroomsBounded.MOD_ID, "failed_escape");

    /**
     * Grant an advancement to a player
     */
    public static void grant(ServerPlayerEntity player, Identifier advancementId) {
        AdvancementEntry advancement = player.getServer()
                .getAdvancementLoader()
                .get(advancementId);

        if (advancement != null) {
            AdvancementProgress progress = player.getAdvancementTracker().getProgress(advancement);
            if (!progress.isDone()) {
                for (String criterion : progress.getUnobtainedCriteria()) {
                    player.getAdvancementTracker().grantCriterion(advancement, criterion);
                }
            }
        }
    }
}