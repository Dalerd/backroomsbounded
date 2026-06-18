package net.dalerd.backroomsbounded.entity.bacterium;

import net.dalerd.backroomsbounded.block.ModBlocks;
import net.dalerd.backroomsbounded.event.CarpetSoundReducer;
import net.dalerd.backroomsbounded.sanity.SanityManager;
import net.dalerd.backroomsbounded.sound.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class BacteriumAI extends Goal {

    private final BacteriumEntity bacterium;
    private final Random random = new Random();
    private PlayerEntity targetPlayer;
    private int checkLockerTimer = 0;
    private int attackCooldown = 0;
    private int existenceTicks = 0;
    private int teleportCooldown = 0;
    private int randomAppearCooldown = 0;
    private int mimicSoundCooldown = 0;
    private int sprintBoostCooldown = 0;
    private int stuckTimer = 0;
    private BlockPos lastPosition = null;
    private int stuckSpinCounter = 0;
    private boolean wasRunning = false;
    private int playerAboveTimer = 0;
    private int observingTimer = 0;
    private boolean isObserving = false;
    private boolean isStalking = false;
    private int stalkTimer = 0;
    private BlockPos stalkPosition = null;
    private int peekCooldown = 0;
    private int footstepCooldown = 0;
    private int breathingCooldown = 0;
    private static final int MAX_DISTANCE_CHUNKS = 20;

    private BlockPos lastHeardSound = null;
    private BlockPos lastSmelledLoot = null;
    private int scentTrackingTimer = 0;
    private static final int SCENT_DURATION = 600;

    private int shroomInvestigationCooldown = 0;
    private boolean isInvestigatingShroom = false;
    private int failedTeleportAttempts = 0;
    private BlockPos lastTeleportTarget = null;

    private static final SoundEvent[] MIMIC_SOUNDS = {
            SoundEvents.ENTITY_CREEPER_PRIMED, SoundEvents.ENTITY_VILLAGER_AMBIENT, SoundEvents.ENTITY_VILLAGER_HURT,
            SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundEvents.ENTITY_ZOMBIE_HURT, SoundEvents.ENTITY_CAT_AMBIENT,
            SoundEvents.ENTITY_CAT_HISS, SoundEvents.ENTITY_WOLF_GROWL, SoundEvents.ENTITY_WOLF_HOWL,
            SoundEvents.ENTITY_SKELETON_AMBIENT, SoundEvents.ENTITY_SPIDER_AMBIENT, SoundEvents.ENTITY_ENDERMAN_SCREAM,
            SoundEvents.ENTITY_ENDERMAN_STARE, SoundEvents.ENTITY_PHANTOM_AMBIENT, SoundEvents.ENTITY_WITCH_AMBIENT,
            SoundEvents.BLOCK_CHEST_OPEN, SoundEvents.BLOCK_CHEST_CLOSE, SoundEvents.BLOCK_WOODEN_DOOR_OPEN,
            SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundEvents.BLOCK_IRON_DOOR_CLOSE,
            SoundEvents.BLOCK_BARREL_CLOSE, SoundEvents.BLOCK_BARREL_OPEN, SoundEvents.BLOCK_CHAIN_BREAK
    };

    public BacteriumAI(BacteriumEntity bacterium) {
        this.bacterium = bacterium;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.TARGET));
    }

    @Override
    public boolean canStart() { return true; }

    @Override
    public void tick() {
        existenceTicks++;
        if (attackCooldown > 0) attackCooldown--;
        if (teleportCooldown > 0) teleportCooldown--;
        if (randomAppearCooldown > 0) randomAppearCooldown--;
        if (mimicSoundCooldown > 0) mimicSoundCooldown--;
        if (sprintBoostCooldown > 0) { sprintBoostCooldown--; if (sprintBoostCooldown <= 0) bacterium.setSprintBoost(false); }
        if (scentTrackingTimer > 0) scentTrackingTimer--;
        if (shroomInvestigationCooldown > 0) shroomInvestigationCooldown--;
        if (observingTimer > 0) observingTimer--;
        if (peekCooldown > 0) peekCooldown--;
        if (footstepCooldown > 0) footstepCooldown--;
        if (breathingCooldown > 0) breathingCooldown--;

        // =========================================
        // BREAK OUT OF VEHICLES
        // =========================================
        if (bacterium.hasVehicle()) {
            Entity vehicle = bacterium.getVehicle();
            bacterium.stopRiding();
            vehicle.discard();
            bacterium.getWorld().playSound(null, bacterium.getBlockPos(),
                    SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1.0f, 0.8f);
        }

        // Enhanced stuck detection
        if (lastPosition != null && bacterium.getBlockPos().equals(lastPosition)) {
            stuckTimer++;
            if (stuckTimer > 20) stuckSpinCounter++;
        } else {
            stuckTimer = 0;
            stuckSpinCounter = 0;
        }
        lastPosition = bacterium.getBlockPos();

        if (stuckTimer > 60 || stuckSpinCounter > 40) {
            if (targetPlayer != null) {
                teleportToObservePoint(targetPlayer);
            } else if (isInvestigatingShroom) {
                BlockPos suspicious = bacterium.getSuspiciousLocation();
                if (suspicious != null && !suspicious.equals(BlockPos.ORIGIN)) {
                    BlockPos tp = findSafePosition(suspicious.add(random.nextInt(10) - 5, 0, random.nextInt(10) - 5));
                    if (tp != null) teleportBacterium(tp);
                }
            } else {
                wanderAround();
            }
            stuckTimer = 0;
            stuckSpinCounter = 0;
            return;
        }

        if (randomAppearCooldown <= 0) tryRandomAppearance();

        targetPlayer = findNearestPlayer();

        // =========================================
        // SHROOM DETECTION
        // =========================================
        BlockPos suspicious = bacterium.getSuspiciousLocation();
        if (suspicious != null && !suspicious.equals(BlockPos.ORIGIN) && shroomInvestigationCooldown <= 0) {
            boolean isCloseToPlayer = targetPlayer != null && bacterium.squaredDistanceTo(targetPlayer) < 100;
            if (!isCloseToPlayer) {
                isInvestigatingShroom = true;
                investigateSuspiciousLocation();
                if (bacterium.getBlockPos().getSquaredDistance(suspicious) < 9) {
                    bacterium.setSuspiciousLocation(BlockPos.ORIGIN);
                    shroomInvestigationCooldown = 200;
                    bacterium.setRunning(false);
                    isInvestigatingShroom = false;
                    failedTeleportAttempts = 0;
                    lastTeleportTarget = null;
                }
                if (targetPlayer == null) return;
                if (bacterium.squaredDistanceTo(targetPlayer) >= 100) return;
            }
        } else {
            isInvestigatingShroom = false;
        }

        if (targetPlayer == null) {
            bacterium.setCheckingLocker(false); bacterium.setRunning(false); bacterium.setSprintBoost(false);
            wasRunning = false; isObserving = false; isStalking = false; wanderAround(); return;
        }

        double distance = bacterium.squaredDistanceTo(targetPlayer);
        int panic = getPlayerPanic(targetPlayer);
        float diffMult = getDifficultyMultiplier();
        float darkMult = getDarknessActivityMultiplier();
        float learningAggression = getLearningAggressionMultiplier() * diffMult;
        boolean isSneaking = targetPlayer.isSneaking();

        updateCrouchState();

        if (existenceTicks % 40 == 0) scanForPlayerActivity();

        double maxDistance = MAX_DISTANCE_CHUNKS * 16;
        if (distance > maxDistance * maxDistance) { teleportToObservePoint(targetPlayer); return; }

        float mimicChance = isFullyLearned() ? 0.3f : 1.0f;
        if (mimicSoundCooldown <= 0 && !isPlayerLookingAt(targetPlayer) && distance > 16 && random.nextFloat() < mimicChance)
            mimicRandomSound();

        int smellRange = (int)(getSmellRange(panic) * learningAggression);
        smellRange = applyLeatherArmorSmellBoost(targetPlayer, smellRange);
        boolean canSmell = distance < (smellRange * smellRange);

        int hearingChunks = (int)(getHearingRange(panic) * learningAggression);
        if (CarpetSoundReducer.isOnQuietSurface(targetPlayer)) {
            hearingChunks = (int)(hearingChunks * 0.5f);
        }
        boolean canHear = distance < (hearingChunks * 16 * hearingChunks * 16);
        if (isSneaking) {
            if (!bacterium.canBreakBlocks()) canSmell = distance < ((smellRange / 2) * (smellRange / 2));
            int reducedChunks = hearingChunks / 2;
            canHear = distance < (reducedChunks * 16 * reducedChunks * 16);
        }
        if (shroomInvestigationCooldown > 0) { canSmell = true; canHear = true; }

        if (scentTrackingTimer > 0 && !canSmell && !canHear) { followScentTrail(); return; }
        if (!canSmell && !canHear && scentTrackingTimer <= 0) {
            bacterium.setRunning(false); bacterium.setSprintBoost(false);
            isObserving = false; isStalking = false; wanderAround(); checkNearbyLockers(); return;
        }

        float scentBonus = (scentTrackingTimer > 0 && (lastSmelledLoot != null || lastHeardSound != null)) ? 1.8f : 1.0f;
        if (stuckTimer > 40) { teleportToObservePoint(targetPlayer); stuckTimer = 0; return; }

        boolean playerLooking = isPlayerLookingAt(targetPlayer);
        boolean pathClear = hasClearPath(targetPlayer);

        // =========================================
        // ENHANCED STALKING MODE
        // =========================================
        if (!isStalking && !isObserving && pathClear && distance > 64 && distance < 576 && !playerLooking && random.nextFloat() < 0.08f) {
            startStalking(targetPlayer);
        }

        if (isStalking) {
            stalkTimer--;
            if (distance < 16 || (playerLooking && distance < 64)) {
                isStalking = false;
                isObserving = false;
                stalkTimer = 0;
                bacterium.getWorld().playSound(null, bacterium.getBlockPos(),
                        ModSounds.BACTERIUM_ROAR, SoundCategory.HOSTILE, 1.5f, 0.5f);
                runToPlayer();
                trySprintBoost(panic);
                return;
            }
            if (stalkTimer <= 0) {
                isStalking = false;
                stalkTimer = 0;
                runToPlayer();
                return;
            }
            stalkPlayer(targetPlayer, distance, playerLooking);
            return;
        }

        // =========================================
        // OBSERVE MODE
        // =========================================
        if (isObserving && observingTimer <= 0) {
            if (distance > 144 && distance < 576 && !playerLooking) {
                bacterium.setRunning(false);
                bacterium.getLookControl().lookAt(targetPlayer);
                if (random.nextFloat() < 0.02f) teleportToObservePoint(targetPlayer);
                observingTimer = 30;
                return;
            } else {
                isObserving = false;
            }
        }

        if (!isObserving && !pathClear && distance > 144 && distance < 576 && !playerLooking && random.nextFloat() < 0.15f) {
            isObserving = true;
            teleportToObservePoint(targetPlayer);
            observingTimer = 60;
            return;
        }

        if (bacterium.canBreakBlocks()) {
            if (!pathClear && bacterium.isRunning()) breakBlocksInPath();
            if (bacterium.horizontalCollision) breakBlockInFront();
            if (existenceTicks % 20 == 0 && targetPlayer != null && !pathClear) breakBlocksInPath();
        }

        if (pathClear) {
            isObserving = false;
            isStalking = false;
            runToPlayer(); trySprintBoost(panic);
        }
        else if (!playerLooking && teleportCooldown <= 0 && distance > 25) {
            teleportScareAttack(targetPlayer);
            teleportCooldown = 150;
        }
        else if (isFullyLearned() && !pathClear) { teleportToObservePoint(targetPlayer); }
        else { navigateAroundObstacle(); }

        if (distance > 625) {
            float hunterChance = getHunterChance(panic) * learningAggression * darkMult * scentBonus;
            if (random.nextFloat() > Math.min(hunterChance, 0.95f)) {
                bacterium.setRunning(false); bacterium.setSprintBoost(false);
                if (teleportCooldown <= 0 && !playerLooking) { teleportToRandomLocation(); teleportCooldown = 200; }
            }
        }
        if (teleportCooldown <= 0 && !playerLooking && distance > 64) {
            teleportScareAttack(targetPlayer);
            teleportCooldown = 200;
        }

        float floorChance = isFullyLearned() ? 0.9f : 0.7f;
        if (Math.abs(targetPlayer.getY() - bacterium.getY()) > 8) {
            if (random.nextFloat() < floorChance * darkMult) {
                bacterium.setRunning(false); bacterium.setSprintBoost(false);
                isObserving = false; isStalking = false; teleportToPlayerFloor(); return;
            }
        }

        handleVerticalPursuit();

        double attackRange = isFullyLearned() ? 4 : 3;
        if (distance < (attackRange * attackRange) && attackCooldown <= 0) {
            isObserving = false;
            isStalking = false;
            if (isPlayerInLocker()) {
                bacterium.setRunning(false); bacterium.setSprintBoost(false); handleLockerInteraction();
            } else {
                attackPlayer();
            }
        }
    }

    // =========================================
    // ENHANCED STALKING SYSTEM
    // =========================================
    private void startStalking(PlayerEntity player) {
        isStalking = true;
        isObserving = false;
        stalkTimer = 600 + random.nextInt(600);
        stalkPosition = null;
        peekCooldown = 0;
        footstepCooldown = 0;
        breathingCooldown = 0;
        bacterium.setRunning(false);
        bacterium.setSprintBoost(false);
        teleportToStalkPosition(player);
    }

    private void stalkPlayer(PlayerEntity player, double distance, boolean playerLooking) {
        bacterium.setRunning(false);
        bacterium.getLookControl().lookAt(player);

        breathingCooldown--;
        if (breathingCooldown <= 0 && distance < 256) {
            float vol = distance < 100 ? 0.4f : 0.2f;
            bacterium.getWorld().playSound(null, bacterium.getBlockPos(),
                    SoundEvents.ENTITY_WARDEN_AMBIENT, SoundCategory.HOSTILE, vol, 0.3f + random.nextFloat() * 0.2f);
            breathingCooldown = 60 + random.nextInt(40);
        }

        footstepCooldown--;
        if (footstepCooldown <= 0 && random.nextFloat() < 0.3f) {
            BlockPos soundPos = bacterium.getBlockPos().add(random.nextInt(6)-3, 0, random.nextInt(6)-3);
            bacterium.getWorld().playSound(null, soundPos,
                    SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.HOSTILE, 0.15f, 0.5f);
            footstepCooldown = 40 + random.nextInt(60);
        }

        peekCooldown--;
        if (peekCooldown <= 0 && !playerLooking && distance > 49 && random.nextFloat() < 0.15f) {
            peekAtPlayer(player);
            peekCooldown = 100 + random.nextInt(100);
        }

        if (random.nextFloat() < 0.05f || stalkPosition == null) {
            if (playerLooking && distance < 144 && random.nextFloat() < 0.3f) {
                teleportToStalkPosition(player);
            } else if (random.nextFloat() < 0.4f) {
                stalkPosition = null;
            }
        }

        if (player.isSprinting() && distance > 144) {
            teleportToStalkPosition(player);
        }

        if (random.nextFloat() < 0.02f) mimicRandomSound();
    }

    private void peekAtPlayer(PlayerEntity player) {
        Vec3d playerLook = player.getRotationVec(1.0f);
        int peekDist = 6 + random.nextInt(5);
        BlockPos peekPos = player.getBlockPos().add((int)(playerLook.x * peekDist), 0, (int)(playerLook.z * peekDist));
        for (int attempt = 0; attempt < 8; attempt++) {
            BlockPos checkPos = peekPos.add(random.nextInt(4)-2, 0, random.nextInt(4)-2);
            BlockPos safePos = findSafePosition(checkPos);
            if (safePos != null) {
                boolean hasCornerCover = false;
                for (var dir : new BlockPos[]{checkPos.north(), checkPos.south(), checkPos.east(), checkPos.west()}) {
                    if (bacterium.getWorld().getBlockState(dir).isSolid()) { hasCornerCover = true; break; }
                }
                if (hasCornerCover) {
                    teleportBacterium(new BlockPos(safePos.getX(), player.getBlockY(), safePos.getZ()));
                    bacterium.setRunning(false);
                    bacterium.getLookControl().lookAt(player);
                    stalkPosition = safePos;
                    bacterium.getWorld().playSound(null, safePos,
                            SoundEvents.BLOCK_STONE_STEP, SoundCategory.HOSTILE, 0.1f, 0.3f);
                    return;
                }
            }
        }
    }

    private void teleportToStalkPosition(PlayerEntity player) {
        int angle = random.nextInt(360);
        int dist = 12 + random.nextInt(9);
        double rad = Math.toRadians(angle);
        int offsetX = (int)(Math.cos(rad) * dist);
        int offsetZ = (int)(Math.sin(rad) * dist);
        BlockPos targetPos = player.getBlockPos().add(offsetX, 0, offsetZ);
        for (int attempt = 0; attempt < 12; attempt++) {
            BlockPos checkPos = targetPos.add(random.nextInt(8)-4, 0, random.nextInt(8)-4);
            BlockPos safePos = findSafePosition(checkPos);
            if (safePos != null) {
                int coverScore = 0;
                for (var dir : new BlockPos[]{checkPos.north(), checkPos.south(), checkPos.east(), checkPos.west()}) {
                    if (bacterium.getWorld().getBlockState(dir).isSolid()) coverScore++;
                }
                if (coverScore >= 2 || random.nextFloat() < 0.4f) {
                    teleportBacterium(new BlockPos(safePos.getX(), player.getBlockY(), safePos.getZ()));
                    bacterium.setRunning(false);
                    bacterium.getLookControl().lookAt(player);
                    stalkPosition = safePos;
                    bacterium.getWorld().playSound(null, safePos,
                            SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.1f, 0.5f);
                    return;
                }
            }
        }
    }

    // =========================================
    // SCARE ATTACK
    // =========================================
    private void teleportScareAttack(PlayerEntity player) {
        Vec3d lookVec = player.getRotationVec(1.0f);
        BlockPos targetPos = player.getBlockPos().add((int)(lookVec.x * 8), 0, (int)(lookVec.z * 8));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos checkPos = targetPos.add(dx, 0, dz);
                BlockPos midPoint = new BlockPos(
                        (checkPos.getX() + player.getBlockX()) / 2, player.getBlockY(),
                        (checkPos.getZ() + player.getBlockZ()) / 2);
                if (bacterium.getWorld().getBlockState(midPoint).isSolid()) {
                    BlockPos safePos = findSafePosition(checkPos);
                    if (safePos != null) {
                        teleportBacterium(new BlockPos(safePos.getX(), player.getBlockY(), safePos.getZ()));
                        bacterium.getWorld().playSound(null, safePos,
                                SoundEvents.ENTITY_ENDERMAN_STARE, SoundCategory.HOSTILE, 0.3f, 0.3f);
                        return;
                    }
                }
            }
        }
        teleportToObservePoint(player);
    }

    // =========================================
    // OBSERVE
    // =========================================
    private void teleportToObservePoint(PlayerEntity player) {
        int angle = random.nextInt(360);
        int dist = 14 + random.nextInt(5);
        double rad = Math.toRadians(angle);
        int offsetX = (int)(Math.cos(rad) * dist);
        int offsetZ = (int)(Math.sin(rad) * dist);
        BlockPos targetPos = player.getBlockPos().add(offsetX, 0, offsetZ);
        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos checkPos = targetPos.add(random.nextInt(6)-3, 0, random.nextInt(6)-3);
            BlockPos safePos = findSafePosition(checkPos);
            if (safePos != null) {
                boolean hasCover = false;
                for (var dir : new BlockPos[]{checkPos.north(), checkPos.south(), checkPos.east(), checkPos.west()}) {
                    if (bacterium.getWorld().getBlockState(dir).isSolid()) { hasCover = true; break; }
                }
                if (hasCover || random.nextFloat() < 0.3f) {
                    teleportBacterium(new BlockPos(safePos.getX(), player.getBlockY(), safePos.getZ()));
                    isObserving = true;
                    observingTimer = 60;
                    bacterium.setRunning(false);
                    bacterium.getLookControl().lookAt(player);
                    return;
                }
            }
        }
    }

    // =========================================
    // CROUCHING SYSTEM
    // =========================================
    private void updateCrouchState() {
        BlockPos abovePos = bacterium.getBlockPos().up(2);
        BlockState aboveState = bacterium.getWorld().getBlockState(abovePos);
        if (aboveState.isSolid() || !bacterium.getWorld().getBlockState(abovePos.up()).isAir()) {
            bacterium.setCrouching(true);
        } else {
            bacterium.setCrouching(false);
        }
    }

    private int applyLeatherArmorSmellBoost(PlayerEntity player, int baseRange) {
        int leatherPieces = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack armor = player.getEquippedStack(slot);
                if (armor.getItem() == Items.LEATHER_BOOTS || armor.getItem() == Items.LEATHER_LEGGINGS ||
                        armor.getItem() == Items.LEATHER_CHESTPLATE || armor.getItem() == Items.LEATHER_HELMET) leatherPieces++;
            }
        }
        if (leatherPieces >= 4) return baseRange * 3;
        if (leatherPieces >= 1) return (int)(baseRange * 1.5f);
        return baseRange;
    }

    private void handleVerticalPursuit() {
        if (targetPlayer == null) return;
        double yDiff = targetPlayer.getY() - bacterium.getY();
        if (yDiff > 2) {
            playerAboveTimer++;
            if (playerAboveTimer > 40) {
                if (bacterium.canBreakBlocks()) breakBlocksUpward();
                else teleportToPlayerLevel();
                playerAboveTimer = 0;
            }
        } else playerAboveTimer = 0;
    }

    private void breakBlocksUpward() {
        if (targetPlayer == null) return;
        BlockPos bacteriumPos = bacterium.getBlockPos();
        for (int y = 0; y < 5; y++) {
            BlockPos checkPos = bacteriumPos.add(0, y + 1, 0);
            BlockState state = bacterium.getWorld().getBlockState(checkPos);
            if (!state.isAir() && state.getHardness(bacterium.getWorld(), checkPos) >= 0 && !isNaturalBackroomsBlock(state)) {
                bacterium.getWorld().breakBlock(checkPos, true); bacterium.incrementBlocksBroken();
            }
        }
    }

    private void teleportToPlayerLevel() {
        if (targetPlayer == null) return;
        BlockPos safePos = findSafePosition(new BlockPos(targetPlayer.getBlockX() + random.nextInt(4) - 2, targetPlayer.getBlockY(), targetPlayer.getBlockZ() + random.nextInt(4) - 2));
        if (safePos != null) teleportBacterium(new BlockPos(safePos.getX(), targetPlayer.getBlockY(), safePos.getZ()));
    }

    private void breakBlocksInPath() {
        if (targetPlayer == null) return;
        BlockPos bacteriumPos = bacterium.getBlockPos();
        BlockPos playerPos = targetPlayer.getBlockPos();
        int dx = Integer.signum(playerPos.getX() - bacteriumPos.getX()), dz = Integer.signum(playerPos.getZ() - bacteriumPos.getZ());
        for (int y = 0; y < 3; y++) {
            BlockPos checkPos = bacteriumPos.add(dx, y, dz);
            BlockState state = bacterium.getWorld().getBlockState(checkPos);
            if (!state.isAir() && state.getHardness(bacterium.getWorld(), checkPos) >= 0 && !isNaturalBackroomsBlock(state)) {
                bacterium.getWorld().breakBlock(checkPos, true); bacterium.incrementBlocksBroken();
            }
        }
    }

    private void breakBlockInFront() {
        for (int y = 0; y < 3; y++) {
            BlockPos frontPos = bacterium.getBlockPos().offset(bacterium.getHorizontalFacing()).up(y);
            BlockState state = bacterium.getWorld().getBlockState(frontPos);
            if (!state.isAir() && state.getHardness(bacterium.getWorld(), frontPos) >= 0 && !isNaturalBackroomsBlock(state)) {
                bacterium.getWorld().breakBlock(frontPos, true); bacterium.incrementBlocksBroken();
            }
        }
    }

    private boolean isNaturalBackroomsBlock(BlockState state) {
        return state.isOf(ModBlocks.WALLPAPER_BLOCK) || state.isOf(ModBlocks.BACKBOARD_BLOCK) || state.isOf(Blocks.YELLOW_TERRACOTTA) ||
                state.isOf(ModBlocks.STAINED_WALLPAPER_BLOCK) || state.isOf(ModBlocks.WET_WALLPAPER_BLOCK) ||
                state.isOf(ModBlocks.MOLDY_WALLPAPER_BLOCK) || state.isOf(ModBlocks.MOLD_INFECTED_WALLPAPER_BLOCK) ||
                state.isOf(ModBlocks.SPONGE_WALLPAPER_BLOCK) || state.isOf(ModBlocks.TORN_WALLPAPER_BLOCK) ||
                state.isOf(ModBlocks.STAINED_TORN_WALLPAPER_BLOCK) || state.isOf(ModBlocks.WET_TORN_WALLPAPER_BLOCK) ||
                state.isOf(ModBlocks.MOLDY_TORN_WALLPAPER_BLOCK) || state.isOf(ModBlocks.MOLD_INFECTED_TORN_WALLPAPER_BLOCK);
    }

    private float getDifficultyMultiplier() {
        return switch (bacterium.getWorld().getDifficulty()) { case EASY -> 0.7f; case NORMAL -> 1.0f; case HARD -> 1.5f; default -> 1.0f; };
    }

    private boolean hasClearPath(PlayerEntity player) {
        BlockPos bp = bacterium.getBlockPos(), pp = player.getBlockPos();
        int dx = Integer.signum(pp.getX() - bp.getX()), dz = Integer.signum(pp.getZ() - bp.getZ());
        BlockPos cp = bp;
        int steps = Math.min((int)Math.sqrt(bp.getSquaredDistance(pp)), 30);
        for (int i = 0; i < steps; i++) {
            if (Math.abs(pp.getX() - cp.getX()) > Math.abs(pp.getZ() - cp.getZ())) cp = cp.add(dx, 0, 0);
            else cp = cp.add(0, 0, dz);
            BlockState s = bacterium.getWorld().getBlockState(cp);
            boolean canFit = !s.isSolid() || (bacterium.isCrouching() && !bacterium.getWorld().getBlockState(cp.up()).isSolid());
            if (bacterium.canBreakBlocks() && !isNaturalBackroomsBlock(s) && s.isSolid()) continue;
            if (s.isSolid() && !canFit) return false;
            if (!bacterium.isCrouching() && (bacterium.getWorld().getBlockState(cp.up()).isSolid() || bacterium.getWorld().getBlockState(cp.up(2)).isSolid())) return false;
        }
        return true;
    }

    private void navigateAroundObstacle() {
        if (targetPlayer == null) return;
        bacterium.setRunning(true);
        int dx = (int)(targetPlayer.getZ() - bacterium.getZ()), dz = (int)(targetPlayer.getX() - bacterium.getX());
        BlockPos[] tries = {
                bacterium.getBlockPos().add(-Integer.signum(dx), 0, Integer.signum(dz)),
                bacterium.getBlockPos().add(Integer.signum(dx), 0, -Integer.signum(dz)),
                bacterium.getBlockPos().add(Integer.signum(dx), 0, Integer.signum(dz)),
                bacterium.getBlockPos().add(-Integer.signum(dx), 0, -Integer.signum(dz))
        };
        for (BlockPos tryPos : tries) {
            BlockState state = bacterium.getWorld().getBlockState(tryPos);
            boolean canFit = !state.isSolid() || (bacterium.isCrouching() && !bacterium.getWorld().getBlockState(tryPos.up()).isSolid());
            if (canFit || (bacterium.canBreakBlocks() && !isNaturalBackroomsBlock(state))) {
                bacterium.getNavigation().startMovingTo(tryPos.getX(), tryPos.getY(), tryPos.getZ(), 1.0);
                bacterium.getLookControl().lookAt(targetPlayer);
                return;
            }
        }
        teleportToObservePoint(targetPlayer);
        bacterium.getLookControl().lookAt(targetPlayer);
    }

    private void wanderAround() {
        if (bacterium.getNavigation().isIdle()) {
            BlockPos rp = bacterium.getBlockPos().add(random.nextInt(32)-16, 0, random.nextInt(32)-16);
            for (int y = 248; y >= 1; y--) {
                BlockPos cp = new BlockPos(rp.getX(), y, rp.getZ());
                if (bacterium.getWorld().getBlockState(cp.down()).isSolid() && bacterium.getWorld().getBlockState(cp).isAir()) {
                    bacterium.getNavigation().startMovingTo(cp.getX(), cp.getY(), cp.getZ(), 0.5); break;
                }
            }
        }
    }

    private void scanForPlayerActivity() {
        if (targetPlayer == null) return;
        BlockPos pp = targetPlayer.getBlockPos();
        int lc = 0; double tx = 0, tz = 0;
        for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) {
            BlockPos cp = pp.add(dx, 0, dz);
            List<ItemEntity> items = bacterium.getWorld().getEntitiesByClass(ItemEntity.class, new Box(cp).expand(1), i -> true);
            if (!items.isEmpty()) { lc += items.size(); tx += cp.getX(); tz += cp.getZ(); }
            BlockState s = bacterium.getWorld().getBlockState(cp);
            if (s.isAir() && bacterium.getWorld().getBlockState(cp.down()).isSolid() && !isWallPosition(cp)) {
                lastHeardSound = cp; scentTrackingTimer = SCENT_DURATION;
            }
        }
        if (lc > 0) {
            lastSmelledLoot = new BlockPos((int)(tx/lc), pp.getY(), (int)(tz/lc));
            scentTrackingTimer = SCENT_DURATION + (lc*40); if (lc >= 5) scentTrackingTimer += SCENT_DURATION;
        }
    }

    private boolean isWallPosition(BlockPos p) {
        return bacterium.getWorld().getBlockState(p.north()).isSolid() || bacterium.getWorld().getBlockState(p.south()).isSolid() ||
                bacterium.getWorld().getBlockState(p.east()).isSolid() || bacterium.getWorld().getBlockState(p.west()).isSolid();
    }

    private void followScentTrail() {
        BlockPos t = lastSmelledLoot != null ? lastSmelledLoot : lastHeardSound;
        if (t != null) {
            bacterium.getNavigation().startMovingTo(t.getX(), t.getY(), t.getZ(), 1.0);
            bacterium.setRunning(true);
            if (bacterium.getBlockPos().getSquaredDistance(t) < 4) { lastSmelledLoot = null; lastHeardSound = null; }
        }
    }

    private void investigateSuspiciousLocation() {
        BlockPos suspicious = bacterium.getSuspiciousLocation();
        if (suspicious != null && !suspicious.equals(BlockPos.ORIGIN)) {
            double dist = bacterium.getBlockPos().getSquaredDistance(suspicious);
            if (dist < 9) {
                bacterium.setSuspiciousLocation(BlockPos.ORIGIN);
                bacterium.setRunning(false);
                shroomInvestigationCooldown = 200;
                isInvestigatingShroom = false;
                failedTeleportAttempts = 0;
                lastTeleportTarget = null;
            } else if (dist < 1024) {
                bacterium.getNavigation().startMovingTo(suspicious.getX(), suspicious.getY(), suspicious.getZ(), 1.3);
                bacterium.setRunning(true);
                bacterium.getLookControl().lookAt(suspicious.getX(), suspicious.getY(), suspicious.getZ());
                teleportCooldown = 100;
            } else if (failedTeleportAttempts < 3) {
                BlockPos tp = findSafePosition(suspicious.add(random.nextInt(16) - 8, 0, random.nextInt(16) - 8));
                if (tp != null && !tp.equals(lastTeleportTarget)) {
                    teleportBacterium(tp);
                    lastTeleportTarget = tp;
                    teleportCooldown = 200;
                    failedTeleportAttempts = 0;
                } else { failedTeleportAttempts++; }
            } else {
                bacterium.getNavigation().startMovingTo(suspicious.getX(), suspicious.getY(), suspicious.getZ(), 1.3);
                bacterium.setRunning(true);
                teleportCooldown = 100;
                failedTeleportAttempts = 0;
            }
        }
    }

    private boolean isFullyLearned() { return bacterium.getLearningLevel() >= 3; }
    private float getLearningAggressionMultiplier() { return 1.0f + (bacterium.getLearningLevel() * 0.3f); }

    private float getHunterChance(int panic) {
        float bc; if (panic <= 40) bc = 0.5f; else if (panic <= 80) bc = 0.6f + ((panic-41)*0.01f); else bc = 1.0f;
        if (isFullyLearned()) bc = Math.max(bc, 0.7f); return bc;
    }

    private int getLightLevel() { return bacterium.getWorld().getLightLevel(LightType.BLOCK, bacterium.getBlockPos()); }
    private float getDarknessActivityMultiplier() {
        int l = getLightLevel(); if (l <= 2) return 2.0f; if (l <= 4) return 1.5f; if (l <= 7) return 1.2f; return 1.0f;
    }

    private void teleportCloseToPlayer(boolean applyDarkness) {
        if (targetPlayer == null) return;
        int cd = 3+random.nextInt(6);
        int ox = (random.nextBoolean()?1:-1)*cd*16+random.nextInt(16), oz = (random.nextBoolean()?1:-1)*cd*16+random.nextInt(16);
        BlockPos tp = findSafePosition(new BlockPos(targetPlayer.getBlockX()+ox, targetPlayer.getBlockY(), targetPlayer.getBlockZ()+oz));
        if (tp != null) {
            teleportBacterium(new BlockPos(tp.getX(), targetPlayer.getBlockY(), tp.getZ()));
            if (applyDarkness && targetPlayer instanceof ServerPlayerEntity sp)
                sp.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 160, 0, false, false));
        }
        bacterium.setRunning(false); bacterium.setSprintBoost(false);
    }

    private void trySprintBoost(int panic) {
        if (panic >= 80 && random.nextFloat() < 0.03f * getDarknessActivityMultiplier() * getLearningAggressionMultiplier() && sprintBoostCooldown <= 0)
        { bacterium.setSprintBoost(true); sprintBoostCooldown = 40; }
    }

    private void mimicRandomSound() {
        SoundEvent s = MIMIC_SOUNDS[random.nextInt(MIMIC_SOUNDS.length)];
        bacterium.getWorld().playSound(null, bacterium.getBlockPos().add(random.nextInt(8)-4, random.nextInt(3)-1, random.nextInt(8)-4),
                s, SoundCategory.HOSTILE, 0.5f, 0.8f+random.nextFloat()*0.4f);
        mimicSoundCooldown = getLightLevel() <= 4 ? 20+random.nextInt(40) : 40+random.nextInt(80);
    }

    private void runToPlayer() {
        if (targetPlayer == null) return;
        if (!wasRunning) { wasRunning = true; bacterium.getWorld().playSound(null, bacterium.getBlockPos(),
                ModSounds.BACTERIUM_ROAR, SoundCategory.HOSTILE, 1.5f, 0.5f); }
        bacterium.setRunning(true); bacterium.getNavigation().startMovingTo(targetPlayer, 1.2); bacterium.getLookControl().lookAt(targetPlayer);
    }

    private void tryRandomAppearance() {
        List<PlayerEntity> pl = bacterium.getWorld().getEntitiesByClass(PlayerEntity.class, bacterium.getBoundingBox().expand(100),
                p -> !p.isCreative() && !p.isSpectator());
        if (!pl.isEmpty()) {
            if (targetPlayer != null && bacterium.squaredDistanceTo(targetPlayer) < 400) { randomAppearCooldown = 200; return; }
            PlayerEntity rp = pl.get(random.nextInt(pl.size()));
            int cd = 4 + random.nextInt(3);
            BlockPos tp = findSafePosition(rp.getBlockPos().add((random.nextInt(cd*2)-cd)*16, 0, (random.nextInt(cd*2)-cd)*16));
            teleportBacterium(tp);
            for (PlayerEntity p : pl) if (bacterium.squaredDistanceTo(p) < 64 && p instanceof ServerPlayerEntity sp)
                sp.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 160, 0, false, false));
        }
        randomAppearCooldown = (int)(600/getDarknessActivityMultiplier()) + random.nextInt((int)(600/getDarknessActivityMultiplier()));
    }

    private BlockPos findSafePosition(BlockPos pos) {
        for (int y = 248; y >= 1; y--) {
            BlockPos cp = new BlockPos(pos.getX(), y, pos.getZ());
            if (bacterium.getWorld().getBlockState(cp.down()).isSolid() && bacterium.getWorld().getBlockState(cp).isAir()
                    && bacterium.getWorld().getBlockState(cp.up()).isAir()) return cp;
        }
        return null;
    }

    private void teleportBacterium(BlockPos pos) { if (pos != null) bacterium.refreshPositionAndAngles(pos.getX()+0.5, pos.getY(), pos.getZ()+0.5, bacterium.getYaw(), bacterium.getPitch()); }

    private int getSmellRange(int panic) { float dm = getDifficultyMultiplier(); if (panic <= 40) return (int)(3*dm); if (panic <= 70) return (int)(6*dm); return (int)(9*dm); }
    private int getHearingRange(int panic) {
        float dm = getDifficultyMultiplier(); int bc, em = existenceTicks/1200;
        if (em < 5) bc = 3; else if (em < 10) bc = 5; else bc = 7;
        if (panic <= 40) return (int)(bc*dm); if (panic <= 60) return (int)((bc+1)*dm); if (panic <= 80) return (int)((bc+2)*dm); return (int)((bc+4)*dm);
    }
    private int getPlayerPanic(PlayerEntity p) { return p instanceof ServerPlayerEntity sp ? SanityManager.getPanic(sp) : 0; }

    private void teleportToRandomLocation() { if (targetPlayer == null) return; int cd = 2+random.nextInt(2); teleportBacterium(findSafePosition(targetPlayer.getBlockPos().add((random.nextInt(cd*2)-cd)*16, 0, (random.nextInt(cd*2)-cd)*16))); }
    private void teleportToPlayerFloor() { if (targetPlayer == null) return; teleportBacterium(findSafePosition(new BlockPos(targetPlayer.getBlockX()+random.nextInt(16)-8, targetPlayer.getBlockY(), targetPlayer.getBlockZ()+random.nextInt(16)-8))); }
    private boolean isPlayerLookingAt(PlayerEntity p) { Vec3d lv = p.getRotationVec(1.0f), te = bacterium.getPos().subtract(p.getPos()).normalize(); return lv.dotProduct(te) > 0.5; }

    private PlayerEntity findNearestPlayer() {
        Box sb = bacterium.getBoundingBox().expand(500);
        List<PlayerEntity> pl = bacterium.getWorld().getEntitiesByClass(PlayerEntity.class, sb, p -> !p.isCreative() && !p.isSpectator());
        PlayerEntity cl = null; double cd = Double.MAX_VALUE;
        for (PlayerEntity p : pl) { double d = bacterium.squaredDistanceTo(p) / (1+(getPlayerPanic(p)/50.0)); if (d < cd) { cd = d; cl = p; } }
        return cl;
    }

    private boolean isPlayerInLocker() {
        if (targetPlayer == null) return false;
        BlockPos playerPos = targetPlayer.getBlockPos();
        BlockState state = bacterium.getWorld().getBlockState(playerPos);
        BlockState aboveState = bacterium.getWorld().getBlockState(playerPos.up());
        return state.getBlock().getTranslationKey().contains("locker") ||
                aboveState.getBlock().getTranslationKey().contains("locker");
    }

    private void handleLockerInteraction() {
        bacterium.setCheckingLocker(true);
        bacterium.setRunning(false);
        bacterium.setSprintBoost(false);
        if (checkLockerTimer < 80) {
            checkLockerTimer++;
            if (bacterium.getNavigation().isIdle()) {
                BlockPos lockerPos = targetPlayer.getBlockPos();
                BlockPos wanderPos = lockerPos.add(random.nextInt(4)-2, 0, random.nextInt(4)-2);
                bacterium.getNavigation().startMovingTo(wanderPos.getX(), wanderPos.getY(), wanderPos.getZ(), 0.4);
            }
            bacterium.getLookControl().lookAt(targetPlayer);
            return;
        }
        checkLockerTimer = 0;
        if (!bacterium.canOpenLockers()) shakeLocker();
        else breakLocker();
        bacterium.setCheckingLocker(false);
        BlockPos awayPos = targetPlayer.getBlockPos().add(random.nextInt(6)-3, 0, random.nextInt(6)-3);
        bacterium.getNavigation().startMovingTo(awayPos.getX(), awayPos.getY(), awayPos.getZ(), 0.5);
    }

    private void shakeLocker() { BlockPos p = targetPlayer.getBlockPos(); bacterium.getWorld().playSound(null, p, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.HOSTILE, 1.0f, 0.5f); bacterium.swingHand(net.minecraft.util.Hand.MAIN_HAND); bacterium.incrementBlocksBroken(); }
    private void breakLocker() { BlockPos p = targetPlayer.getBlockPos(); bacterium.getWorld().breakBlock(p, true); bacterium.getWorld().playSound(null, p, SoundEvents.ENTITY_IRON_GOLEM_DEATH, SoundCategory.HOSTILE, 1.0f, 1.0f); bacterium.swingHand(net.minecraft.util.Hand.MAIN_HAND); bacterium.incrementBlocksBroken(); }
    private void checkNearbyLockers() { BlockPos bp = bacterium.getBlockPos(); for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) { BlockPos p = bp.add(dx, 0, dz); if (bacterium.getWorld().getBlockState(p).getBlock().getTranslationKey().contains("locker")) { bacterium.getNavigation().startMovingTo(p.getX(), p.getY(), p.getZ(), 0.8); bacterium.setCheckingLocker(true); return; } } bacterium.setCheckingLocker(false); }

    // =========================================
    // ATTACK - Regular hit OR grab at close range
    // =========================================
    private void attackPlayer() {
        if (targetPlayer == null) return;
        if (attackCooldown > 0) return;
        if (bacterium.isGrabbing()) return;

        double distance = bacterium.squaredDistanceTo(targetPlayer);

        // Within 2 blocks = GRAB ATTACK
        if (distance < 4 && random.nextFloat() < 0.3f) { // 30% chance to grab
            startGrabAttack(targetPlayer);
            return;
        }

        // Regular hit
        bacterium.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        float damagePerHit = switch (bacterium.getWorld().getDifficulty()) {
            case EASY, NORMAL -> 8.0f;
            case HARD -> 9.0f;
            default -> 4.0f;
        };
        targetPlayer.damage(bacterium.getDamageSources().mobAttack(bacterium), damagePerHit);
        bacterium.getWorld().playSound(null, bacterium.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 0.5f, 0.5f);
        attackCooldown = 20;
    }

    // =========================================
    // GRAB ATTACK SYSTEM
    // =========================================
    private void startGrabAttack(PlayerEntity player) {
        bacterium.startGrabbing(player);
        bacterium.getWorld().playSound(null, bacterium.getBlockPos(),
                ModSounds.BACTERIUM_ROAR, SoundCategory.HOSTILE, 2.0f, 0.3f);

        // Spawn dark particles around the grab
        if (bacterium.getWorld() instanceof ServerWorld sw) {
            spawnEmergenceParticles(sw, bacterium.getBlockPos());
        }
    }

    public static void spawnEmergenceParticles(ServerWorld world, BlockPos pos) {
        // Spawn dark particles rising from the ground like warden emergence
        for (int i = 0; i < 30; i++) {
            double x = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 2;
            double y = pos.getY() + world.random.nextDouble() * 3;
            double z = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 2;
            world.spawnParticles(
                    net.minecraft.particle.ParticleTypes.SCULK_SOUL,
                    x, y, z, 1, 0, 0.1, 0, 0.02
            );
        }
    }
}
