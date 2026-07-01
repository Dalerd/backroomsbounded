package net.dalerd.backroomsbounded.advancement;

import net.dalerd.backroomsbounded.BackroomsBounded;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class AdvancementManager {

    /**
     * Generic advancement grant method.
     * Awards every remaining criterion if the advancement isn't already completed.
     */
    private static void grant(ServerPlayerEntity player, String advancementId) {

        AdvancementEntry advancement = player.getServer()
                .getAdvancementLoader()
                .get(Identifier.of(BackroomsBounded.MOD_ID, advancementId));

        if (advancement == null) {
            BackroomsBounded.LOGGER.warn("Couldn't find advancement '{}'", advancementId);
            return;
        }

        AdvancementProgress progress = player.getAdvancementTracker().getProgress(advancement);

        if (progress.isDone()) {
            return;
        }

        for (String criterion : progress.getUnobtainedCriteria()) {
            player.getAdvancementTracker().grantCriterion(advancement, criterion);
        }
    }

    // =====================================================
    // ROOT
    // =====================================================

    public static void enterBackrooms(ServerPlayerEntity player) {
        grant(player, ModAdvancements.ROOT);
    }

    // =====================================================
    // ENTITIES
    // =====================================================

    public static void findBacterium(ServerPlayerEntity player) {
        grant(player, ModAdvancements.FIRST_BACTERIUM);
    }

    public static void findMimic(ServerPlayerEntity player) {
        grant(player, ModAdvancements.FIND_MIMIC);
    }

    public static void surviveGrab(ServerPlayerEntity player) {
        grant(player, ModAdvancements.SURVIVE_GRAB);
    }

    // =====================================================
    // LOCATIONS
    // =====================================================

    public static void enterOffice(ServerPlayerEntity player) {
        grant(player, ModAdvancements.ENTER_OFFICE);
    }

    // Optional future advancement
    public static void deepOffice(ServerPlayerEntity player) {
        grant(player, ModAdvancements.DEEP_OFFICE);
    }

    // =====================================================
    // ITEMS / BLOCKS
    // =====================================================

    public static void waterCooler(ServerPlayerEntity player) {
        grant(player, ModAdvancements.FIND_WATER_COOLER);
    }

    // Optional future advancement
    public static void bacteriaShroom(ServerPlayerEntity player) {
        grant(player, ModAdvancements.TRIGGER_BACTERIA_SHROOM);
    }

    // =====================================================
    // ESCAPE
    // =====================================================

    public static void escapeBackrooms(ServerPlayerEntity player) {
        grant(player, ModAdvancements.ESCAPE);
    }

    public static void failEscape(ServerPlayerEntity player) {
        grant(player, ModAdvancements.FAILED_ESCAPE);
    }

    // =====================================================
    // SANITY
    // =====================================================

    public static void panicAttack(ServerPlayerEntity player) {
        grant(player, ModAdvancements.PANIC_ATTACK);
    }
}