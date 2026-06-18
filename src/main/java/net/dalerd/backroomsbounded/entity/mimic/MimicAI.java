package net.dalerd.backroomsbounded.entity.mimic;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.SwordItem;
import net.minecraft.item.AxeItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class MimicAI extends Goal {

    private final MimicEntity mimic;
    private final Random random = new Random();
    private PlayerEntity targetPlayer;
    private int stuckTimer = 0;
    private BlockPos lastPosition;
    private int whisperCooldown = 0;
    private int actionCooldown = 0;
    private boolean waitingForCommand = false;
    private BlockPos commandTargetPos = null;
    private int commandTimer = 0;

    public MimicAI(MimicEntity mimic) {
        this.mimic = mimic;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.TARGET));
    }

    @Override
    public boolean canStart() { return true; }

    @Override
    public void tick() {
        if (whisperCooldown > 0) whisperCooldown--;
        if (actionCooldown > 0) actionCooldown--;
        if (commandTimer > 0) {
            commandTimer--;
            if (commandTargetPos != null) {
                mimic.getNavigation().startMovingTo(commandTargetPos.getX(), commandTargetPos.getY(), commandTargetPos.getZ(), 0.8);
                if (mimic.getBlockPos().getSquaredDistance(commandTargetPos) < 4) commandTargetPos = null;
            }
        }
        mimic.decrementChatCooldown();
        mimic.decrementInventoryCopyCooldown();

        targetPlayer = findNearestPlayer();
        if (targetPlayer == null) { wanderAround(); return; }

        double distance = mimic.squaredDistanceTo(targetPlayer);

        if (lastPosition != null && mimic.getBlockPos().equals(lastPosition)) stuckTimer++;
        else stuckTimer = 0;
        lastPosition = mimic.getBlockPos();

        if (stuckTimer > 60) { teleportNearPlayer(); stuckTimer = 0; }
        if (distance > 900) mimic.setAggressive(true);

        if (mimic.isAggressive()) {
            mimic.getNavigation().startMovingTo(targetPlayer, 1.3);
            mimic.getLookControl().lookAt(targetPlayer);
            if (distance < 4) attackPlayer();
        } else {
            if (distance > 25) mimic.getNavigation().startMovingTo(targetPlayer, 0.8);
            mimic.getLookControl().lookAt(targetPlayer);

            if (whisperCooldown <= 0 && distance < 100) {
                mimic.sendWhisper(targetPlayer);
                whisperCooldown = 200 + random.nextInt(300);
            }
            if (mimic.getInventoryCopyCooldown() <= 0 && distance < 16) mimic.copyInventoryFrom(targetPlayer);
            if (actionCooldown <= 0 && distance < 8) { mimicContainerInteraction(); actionCooldown = 100 + random.nextInt(200); }
        }
    }

    // =========================================
    // COMMAND SYSTEM
    // =========================================
    public void checkForPlayerCommand(String message) {
        if (targetPlayer == null) return;
        String msg = message.toLowerCase().trim();
        double distance = mimic.squaredDistanceTo(targetPlayer);
        if (distance > 225) return;

        if (containsAny(msg, "pick it up", "take it", "grab it", "get it")) {
            pickUpNearbyItems();
            mimic.sendWhisper(targetPlayer, "ok");
            return;
        }
        if (containsAny(msg, "open it", "open that", "open the")) {
            openNearbyContainerOrDoor();
            return;
        }
        if (containsAny(msg, "follow me", "come here", "follow", "come")) {
            mimic.setAggressive(false);
            mimic.sendWhisper(targetPlayer, "ok, i'm coming");
            return;
        }
        if (containsAny(msg, "stay", "stop", "wait here", "dont move")) {
            mimic.getNavigation().stop();
            mimic.sendWhisper(targetPlayer, "ok, i'll wait");
            commandTimer = 200;
            return;
        }
        if (containsAny(msg, "go away", "leave", "get lost", "go")) {
            mimic.setAggressive(true);
            mimic.sendWhisper(targetPlayer, "fine...");
        }
    }

    private void pickUpNearbyItems() {
        if (targetPlayer == null) return;
        BlockPos playerPos = targetPlayer.getBlockPos();
        List<ItemEntity> items = mimic.getWorld().getEntitiesByClass(ItemEntity.class,
                new Box(playerPos).expand(5), item -> true);
        if (!items.isEmpty()) {
            ItemEntity target = items.get(0);
            mimic.getNavigation().startMovingTo(target.getX(), target.getY(), target.getZ(), 1.0);
        }
    }

    private void openNearbyContainerOrDoor() {
        if (targetPlayer == null) return;
        BlockPos mimicPos = mimic.getBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos pos = mimicPos.add(dx, dy, dz);
                    String name = mimic.getWorld().getBlockState(pos).getBlock().getTranslationKey();
                    if (name.contains("chest") || name.contains("barrel") || name.contains("locker") ||
                            name.contains("door") || name.contains("trapdoor") || name.contains("gate")) {
                        mimic.getNavigation().startMovingTo(pos.getX(), pos.getY(), pos.getZ(), 0.8);
                        commandTargetPos = pos;
                        commandTimer = 60;
                        return;
                    }
                }
            }
        }
        mimic.sendWhisper(targetPlayer, "i don't see anything to open");
    }

    private boolean containsAny(String message, String... keywords) {
        for (String k : keywords) if (message.contains(k)) return true;
        return false;
    }

    // =========================================
    // PLAYER DETECTION
    // =========================================
    private PlayerEntity findNearestPlayer() {
        PlayerEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (PlayerEntity player : mimic.getWorld().getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            double dist = mimic.squaredDistanceTo(player);
            if (dist < closestDist && dist < 6400) { closestDist = dist; closest = player; }
        }
        return closest;
    }

    private void attackPlayer() {
        if (targetPlayer == null) return;
        mimic.swingHand(Hand.MAIN_HAND);
        float damage = 2.0f;
        var held = mimic.getMainHandStack();
        if (held.getItem() instanceof SwordItem) damage += 3.0f;
        else if (held.getItem() instanceof AxeItem) damage += 2.0f;
        targetPlayer.damage(mimic.getDamageSources().mobAttack(mimic), damage);
    }

    private void teleportNearPlayer() {
        if (targetPlayer == null) return;
        int ox = random.nextInt(8) - 4, oz = random.nextInt(8) - 4;
        BlockPos tp = targetPlayer.getBlockPos().add(ox, 0, oz);
        mimic.refreshPositionAndAngles(tp.getX() + 0.5, tp.getY(), tp.getZ() + 0.5, mimic.getYaw(), mimic.getPitch());
    }

    private void wanderAround() {
        if (mimic.getNavigation().isIdle()) {
            BlockPos rp = mimic.getBlockPos().add(random.nextInt(32) - 16, 0, random.nextInt(32) - 16);
            mimic.getNavigation().startMovingTo(rp.getX(), rp.getY(), rp.getZ(), 0.5);
        }
    }

    private void mimicContainerInteraction() {
        BlockPos mp = mimic.getBlockPos();
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            BlockPos p = mp.add(dx, 0, dz);
            String n = mimic.getWorld().getBlockState(p).getBlock().getTranslationKey();
            if (n.contains("chest") || n.contains("barrel") || n.contains("locker")) {
                mimic.getNavigation().startMovingTo(p.getX(), p.getY(), p.getZ(), 0.6);
                return;
            }
        }
    }
}