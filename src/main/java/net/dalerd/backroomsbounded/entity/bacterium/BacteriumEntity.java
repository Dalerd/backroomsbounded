package net.dalerd.backroomsbounded.entity.bacterium;

import net.dalerd.backroomsbounded.entity.ModEntities;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BacteriumEntity extends HostileEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final float WALK_SPEED = 0.22f;
    private static final float RUN_SPEED = 0.29f;
    private static final float SPRINT_SPEED = 0.37f;
    private static final float CROUCH_PENALTY = 0.7f;

    // Grab attack system
    private static final TrackedData<Boolean> GRABBING =
            DataTracker.registerData(BacteriumEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private PlayerEntity grabbedPlayer = null;
    private int grabTimer = 0;
    private static final int ESCAPE_CLICKS_NEEDED = 3;
    public int escapeClicks = 0;
    private boolean grabKillTriggered = false;

    private static final TrackedData<Integer> LEARNING_LEVEL =
            DataTracker.registerData(BacteriumEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> BLOCKS_BROKEN =
            DataTracker.registerData(BacteriumEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> CHECKING_LOCKER =
            DataTracker.registerData(BacteriumEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> RUNNING =
            DataTracker.registerData(BacteriumEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SPRINT_BOOST =
            DataTracker.registerData(BacteriumEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> JUMPSCARING =
            DataTracker.registerData(BacteriumEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> CROUCHING =
            DataTracker.registerData(BacteriumEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<BlockPos> SUSPICIOUS_LOCATION =
            DataTracker.registerData(BacteriumEntity.class, TrackedDataHandlerRegistry.BLOCK_POS);

    private int blocksPlacedByPlayer = 0;
    private int doorsOpened = 0;
    private int lockersOpened = 0;

    public BacteriumEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 0;
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 9999)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, WALK_SPEED)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 9)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 100)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 9999);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new BacteriumAI(this));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(LEARNING_LEVEL, 0);
        builder.add(BLOCKS_BROKEN, 0);
        builder.add(CHECKING_LOCKER, false);
        builder.add(RUNNING, false);
        builder.add(SPRINT_BOOST, false);
        builder.add(JUMPSCARING, false);
        builder.add(CROUCHING, false);
        builder.add(SUSPICIOUS_LOCATION, BlockPos.ORIGIN);
        builder.add(GRABBING, false);
    }

    // =========================================
    // GRAB SYSTEM
    // =========================================
    public boolean isGrabbing() { return this.dataTracker.get(GRABBING); }
    public void setGrabbing(boolean grabbing) { this.dataTracker.set(GRABBING, grabbing); }
    public PlayerEntity getGrabbedPlayer() { return grabbedPlayer; }

    public void startGrabbing(PlayerEntity player) {
        this.grabbedPlayer = player;
        this.grabTimer = 0;
        this.escapeClicks = 0;
        this.grabKillTriggered = false;
        this.setGrabbing(true);
        this.setJumpscaring(true);
        this.setVelocity(0, 0, 0);
        player.setVelocity(0, 0, 0);
    }

    public void stopGrabbing() {
        PlayerEntity wasGrabbed = this.grabbedPlayer;
        this.grabbedPlayer = null;
        this.grabTimer = 0;
        this.escapeClicks = 0;
        this.grabKillTriggered = false;
        this.setGrabbing(false);
        this.setJumpscaring(false);
        // Knock player back when released
        if (wasGrabbed != null) {
            wasGrabbed.setVelocity(
                    (this.getX() - wasGrabbed.getX()) * 0.5,
                    0.3,
                    (this.getZ() - wasGrabbed.getZ()) * 0.5
            );
            wasGrabbed.velocityModified = true;
        }
    }

    public void playerClickedToEscape() {
        if (!isGrabbing() || grabbedPlayer == null) return;
        escapeClicks++;
        if (escapeClicks >= ESCAPE_CLICKS_NEEDED) {
            stopGrabbing();
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Handle grab timer
        if (isGrabbing() && grabbedPlayer != null && !this.getWorld().isClient) {
            grabTimer++;

            // Hold player 1 block + 8 pixels (0.5 blocks) above bacterium's feet
            double holdY = this.getY() + 1.5;
            grabbedPlayer.refreshPositionAndAngles(
                    this.getX(), holdY, this.getZ(),
                    grabbedPlayer.getYaw(), grabbedPlayer.getPitch()
            );
            grabbedPlayer.setVelocity(0, 0, 0);
            grabbedPlayer.velocityModified = true;

            // Red static effect via darkness + nausea
            if (grabbedPlayer instanceof ServerPlayerEntity sp) {
                sp.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.DARKNESS, 20, 0, false, false));
                sp.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NAUSEA, 20, 0, false, false));
            }

            // After 3 seconds, trigger kill if not escaped
            if (grabTimer >= 80 && !grabKillTriggered) {
                grabKillTriggered = true;
                grabbedPlayer.damage(this.getDamageSources().mobAttack(this), Float.MAX_VALUE);
                stopGrabbing();
            }
        }
    }

    @Override
    public boolean canStartRiding(Entity vehicle) { return false; }
    @Override
    protected void tickControlled(PlayerEntity controllingPlayer, Vec3d movementInput) {}

    public static boolean existsInWorld(ServerWorld world) {
        return !world.getEntitiesByType(ModEntities.BACTERIUM, entity -> true).isEmpty();
    }

    // =========================================
    // DIFFICULTY SYSTEM
    // =========================================
    public float getDifficultyMultiplier() {
        return switch (this.getWorld().getDifficulty()) {
            case PEACEFUL -> 0.0f; case EASY -> 0.7f; case NORMAL -> 1.0f; case HARD -> 1.5f;
        };
    }

    @Override public boolean canImmediatelyDespawn(double d) { return false; }
    @Override public boolean isPersistent() { return true; }
    @Override public int getDespawnCounter() { return 0; }
    @Override public void setDespawnCounter(int c) {}
    @Override public void checkDespawn() {}

    public boolean isCrouching() { return this.dataTracker.get(CROUCHING); }
    public void setCrouching(boolean c) {
        if (this.dataTracker.get(CROUCHING) != c) {
            this.dataTracker.set(CROUCHING, c);
            this.setPose(c ? EntityPose.CROUCHING : EntityPose.STANDING);
            updateSpeed();
        }
    }

    public void playerOpenedDoor() { doorsOpened++; updateLearning(); }
    public void playerOpenedLocker() { lockersOpened++; updateLearning(); }
    public void playerPlacedBlock() { blocksPlacedByPlayer++; updateLearning(); }
    private void updateLearning() {
        int total = doorsOpened + lockersOpened + blocksPlacedByPlayer;
        this.dataTracker.set(LEARNING_LEVEL, Math.min(total / 5, 3));
    }
    public int getLearningLevel() { return this.dataTracker.get(LEARNING_LEVEL); }
    public boolean canOpenDoors() { return getLearningLevel() >= 1; }
    public boolean canOpenLockers() { return getLearningLevel() >= 2; }
    public boolean canBreakBlocks() { return getLearningLevel() >= 3; }
    public int getBlocksBroken() { return this.dataTracker.get(BLOCKS_BROKEN); }
    public void incrementBlocksBroken() { this.dataTracker.set(BLOCKS_BROKEN, getBlocksBroken() + 1); }
    public boolean isCheckingLocker() { return this.dataTracker.get(CHECKING_LOCKER); }
    public void setCheckingLocker(boolean c) { this.dataTracker.set(CHECKING_LOCKER, c); }
    public boolean isJumpscaring() { return this.dataTracker.get(JUMPSCARING); }
    public void setJumpscaring(boolean j) { this.dataTracker.set(JUMPSCARING, j); }
    public BlockPos getSuspiciousLocation() { return this.dataTracker.get(SUSPICIOUS_LOCATION); }
    public void setSuspiciousLocation(BlockPos p) { this.dataTracker.set(SUSPICIOUS_LOCATION, p); }
    public boolean isRunning() { return this.dataTracker.get(RUNNING); }
    public boolean isSprintBoosting() { return this.dataTracker.get(SPRINT_BOOST); }
    public void setSprintBoost(boolean b) { this.dataTracker.set(SPRINT_BOOST, b); updateSpeed(); }
    public void setRunning(boolean r) { this.dataTracker.set(RUNNING, r); updateSpeed(); }

    private void updateSpeed() {
        EntityAttributeInstance attr = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        float dm = getDifficultyMultiplier();
        float cm = isCrouching() ? CROUCH_PENALTY : 1.0f;
        if (attr != null) {
            if (isSprintBoosting()) attr.setBaseValue(SPRINT_SPEED * dm * cm);
            else if (isRunning()) attr.setBaseValue(RUN_SPEED * dm * cm);
            else attr.setBaseValue(WALK_SPEED * dm * cm);
        }
    }

    @Override public boolean isInvulnerable() { return true; }
    @Override public boolean damage(DamageSource s, float a) { return false; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "ground", 5, this::groundController));
        controllers.add(new AnimationController<>(this, "attack", 5, this::attackController));
        controllers.add(new AnimationController<>(this, "quirk", 5, this::quirkController));
        controllers.add(new AnimationController<>(this, "jumpscare", 5, this::jumpscareController));
    }

    private PlayState groundController(AnimationState<BacteriumEntity> state) {
        if (isDead()) return PlayState.STOP;
        // Keep idle playing during grab so jumpscare overlays on top
        if (isRunning() && !isGrabbing()) {
            state.setAnimation(RawAnimation.begin().thenLoop("animation.bacterium.ground_run"));
        } else if (state.isMoving() && !isGrabbing()) {
            state.setAnimation(RawAnimation.begin().thenLoop("animation.bacterium.ground_walk"));
        } else if (isCheckingLocker()) {
            state.setAnimation(RawAnimation.begin().thenPlay("animation.bacterium.checking_locker"));
        } else {
            state.setAnimation(RawAnimation.begin().thenLoop("animation.bacterium.ground_idle"));
        }
        return PlayState.CONTINUE;
    }

    private PlayState attackController(AnimationState<BacteriumEntity> state) {
        if (handSwinging && !isGrabbing()) {
            state.setAnimation(RawAnimation.begin().thenPlay("animation.bacterium.attack"));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    private PlayState quirkController(AnimationState<BacteriumEntity> state) {
        if (age % 200 < 10 && !isJumpscaring() && !isGrabbing()) {
            state.setAnimation(RawAnimation.begin().thenPlay("animation.bacterium.quirk").thenPlay("animation.bacterium.quirk_1").thenPlay("animation.bacterium.quirk_2"));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    private PlayState jumpscareController(AnimationState<BacteriumEntity> state) {
        if (isGrabbing()) {
            // Overlay jumpscare on top of idle
            state.setAnimation(RawAnimation.begin().thenLoop("animation.bacterium.jumpscare"));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("DoorsOpened", doorsOpened);
        nbt.putInt("LockersOpened", lockersOpened);
        nbt.putInt("BlocksPlaced", blocksPlacedByPlayer);
        nbt.putInt("LearningLevel", getLearningLevel());
        BlockPos s = getSuspiciousLocation();
        nbt.putInt("SuspiciousX", s.getX()); nbt.putInt("SuspiciousY", s.getY()); nbt.putInt("SuspiciousZ", s.getZ());
    }

    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        doorsOpened = nbt.getInt("DoorsOpened");
        lockersOpened = nbt.getInt("LockersOpened");
        blocksPlacedByPlayer = nbt.getInt("BlocksPlaced");
        this.dataTracker.set(LEARNING_LEVEL, nbt.getInt("LearningLevel"));
        if (nbt.contains("SuspiciousX"))
            this.dataTracker.set(SUSPICIOUS_LOCATION, new BlockPos(nbt.getInt("SuspiciousX"), nbt.getInt("SuspiciousY"), nbt.getInt("SuspiciousZ")));
    }
}