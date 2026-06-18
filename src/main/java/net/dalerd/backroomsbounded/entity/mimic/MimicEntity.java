package net.dalerd.backroomsbounded.entity.mimic;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public class MimicEntity extends HostileEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final TrackedData<String> COPIED_PLAYER_NAME =
            DataTracker.registerData(MimicEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Boolean> IS_AGGRESSIVE =
            DataTracker.registerData(MimicEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private UUID copiedPlayerUUID;
    private int chatCooldown = 0;
    private int inventoryCopyCooldown = 0;
    private int verityMessageCount = 0;

    private static final String[] FALLBACK_NAMES = {
            "Verity", "Alex", "Kai", "Rex", "Mark", "Jade", "Sam", "Casey",
            "Morgan", "Riley", "Jordan", "Avery", "Steve", "Yes", "Quinn",
            "Blake", "Taylor", "Drew", "Skylar", "Caseoh"
    };

    public MimicEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 80);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new MimicAI(this));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(COPIED_PLAYER_NAME, "");
        builder.add(IS_AGGRESSIVE, false);
    }

    public void initialize(ServerPlayerEntity playerToCopy) {
        if (playerToCopy != null) {
            setCopiedPlayerName(playerToCopy.getName().getString());
            setCopiedPlayerUUID(playerToCopy.getUuid());
        } else {
            String fallbackName = getRandomFallbackName();
            setCopiedPlayerName(fallbackName);
            setCopiedPlayerUUID(UUID.randomUUID());
        }
        this.setCustomNameVisible(true);
    }

    private String getRandomFallbackName() {
        Random random = new Random();
        if (random.nextFloat() < 0.03f) return "Caseoh";
        String[] commonNames = {
                "Verity", "Alex", "Kai", "Rex", "Mark", "Jade", "Sam", "Casey",
                "Morgan", "Riley", "Jordan", "Avery", "Steve", "Yes", "Quinn",
                "Blake", "Taylor", "Drew", "Skylar"
        };
        return commonNames[random.nextInt(commonNames.length)];
    }

    public String getCopiedPlayerName() { return this.dataTracker.get(COPIED_PLAYER_NAME); }

    public void setCopiedPlayerName(String name) {
        this.dataTracker.set(COPIED_PLAYER_NAME, name);
        this.setCustomName(Text.literal(name));
        this.setCustomNameVisible(true);
    }

    public boolean isAggressive() { return this.dataTracker.get(IS_AGGRESSIVE); }
    public void setAggressive(boolean aggressive) { this.dataTracker.set(IS_AGGRESSIVE, aggressive); }
    public UUID getCopiedPlayerUUID() { return copiedPlayerUUID; }
    public void setCopiedPlayerUUID(UUID uuid) { this.copiedPlayerUUID = uuid; }

    // =========================================
    // CHAT SYSTEM
    // =========================================
    public String getResponseForMessage(String message) {
        String msg = message.toLowerCase().trim();
        String playerName = getCopiedPlayerName();

        if ("Verity".equals(playerName)) return getVerityResponse();
        if ("Alex".equals(playerName)) {
            if (containsAny(msg, "hi", "hello", "hey")) return "yo what's good";
            if (containsAny(msg, "where", "lost")) return "no clue man, same here";
            return pickRandom("yeah", "nah", "idk", "maybe later");
        }
        if ("Kai".equals(playerName)) {
            if (containsAny(msg, "hear", "sound", "noise")) return "you heard that too? something is moving";
            if (containsAny(msg, "scared", "afraid")) return "we need to keep moving, please";
            return pickRandom("did you see that?", "shh, listen", "its close", "dont leave me alone");
        }
        if ("Rex".equals(playerName)) {
            if (containsAny(msg, "fight", "kill", "attack")) return "i'd like to see you try";
            if (containsAny(msg, "scared")) return "good, you should be";
            return pickRandom("what do you want", "make it quick", "im not in the mood", "whatever");
        }
        if ("Mark".equals(playerName)) {
            if (containsAny(msg, "help", "lost")) return "i can help, but there's a price";
            if (containsAny(msg, "exit", "leave", "escape")) return "ive been here for years, theres no exit";
            return pickRandom("i know a shortcut", "follow me", "trust me", "ive seen things here");
        }
        if ("Caseoh".equals(playerName)) return getCaseohResponse(msg);

        if (containsAny(msg, "u good", "you good", "u ok", "you ok")) return pickRandom("yeah just tired", "im fine", "just watching");
        if (containsAny(msg, "hi", "hello", "hey", "sup")) return pickRandom("hey", "hi there", "hello");
        if (containsAny(msg, "hear me", "hear you", "can u hear")) return pickRandom("loud and clear", "yeah i hear you");
        if (containsAny(msg, "follow", "following")) return pickRandom("im not following", "just going the same way");
        if (containsAny(msg, "real", "fake")) return pickRandom("of course im real", "what kind of question is that");
        if (containsAny(msg, "help", "save")) return pickRandom("help isnt coming", "no one can save you");
        if (containsAny(msg, "who are you", "what are you")) return pickRandom("you know my name", "im " + playerName, "dont you recognize me");

        return pickRandom("hmm?", "what?", "yeah", "i dont know", "...", "maybe");
    }

    private String getVerityResponse() {
        verityMessageCount++;
        if (verityMessageCount == 1) return "Hi, I'm Verity, your personal companion. You can ask me anything, I know everything.";
        if (verityMessageCount == 2) return "Something hungry is coming in 3 days.";
        if (verityMessageCount == 3) return "Don't go East.";
        return pickRandom("it's getting closer", "i can hear it breathing", "you should hide",
                "it knows your name now", "the lights are flickering again", "do you feel that?",
                "something is watching us", "i dont like this place", "we're not alone", "it remembers you");
    }

    private String getCaseohResponse(String msg) {
        if (containsAny(msg, "who", "what are you")) return "You met the backrooms specialist";
        if (containsAny(msg, "hungry", "food", "eat")) return "I'm hungry... Chicken Alfredooooo";
        if (containsAny(msg, "lost", "where")) return "I know where I'm going... I think";
        if (containsAny(msg, "scared", "entity", "monster", "creature")) return "Um so there's that um... uh... tall entity, I think we should go to it, ask how it's doing, you know";
        if (containsAny(msg, "store", "shop", "market", "supermarket")) return "I wonder if my supermarket is in here";
        if (containsAny(msg, "hi", "hello", "hey")) return "Gahuk! Oh, hey there";
        return pickRandom("Gahuk", "I know where I'm going", "Chicken Alfredooooo", "I'm hungry",
                "You met the backrooms specialist", "I wonder if my supermarket is in here",
                "Um so there's that um... uh... tall entity, I think we should go to it");
    }

    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) if (message.contains(keyword)) return true;
        return false;
    }

    private String pickRandom(String... options) { return options[new Random().nextInt(options.length)]; }

    public void sendWhisper(PlayerEntity target) {
        sendWhisper(target, null);
    }

    public void sendWhisper(PlayerEntity target, String customMessage) {
        if (chatCooldown > 0) return;

        String playerName = getCopiedPlayerName();
        String msg;

        if (customMessage != null) {
            msg = customMessage;
        } else if ("Caseoh".equals(playerName)) {
            String[] whispers = {"I think I saw a supermarket back there", "You ever had Chicken Alfredo in the backrooms?",
                    "Gahuk... this place is huge", "I wonder if I can order some food here", "My back hurts from all this walking"};
            msg = whispers[new Random().nextInt(whispers.length)];
        } else {
            String[] whispers = {"im right behind you", "dont turn around", "i see you",
                    "where are you going", "wait for me", "you dropped something",
                    "its dark in here", "i dont like this place", "are we lost", "keep walking"};
            msg = whispers[new Random().nextInt(whispers.length)];
        }

        Text whisper = Text.literal("<" + playerName + "> " + msg)
                .formatted(Formatting.GRAY, Formatting.ITALIC);
        target.sendMessage(whisper, false);

        chatCooldown = customMessage != null ? 60 : 200 + new Random().nextInt(400);
    }

    public int getChatCooldown() { return chatCooldown; }
    public void decrementChatCooldown() { if (chatCooldown > 0) chatCooldown--; }
    public int getInventoryCopyCooldown() { return inventoryCopyCooldown; }
    public void decrementInventoryCopyCooldown() { if (inventoryCopyCooldown > 0) inventoryCopyCooldown--; }

    public void copyInventoryFrom(PlayerEntity player) {
        if (inventoryCopyCooldown > 0) return;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                this.equipStack(slot, player.getEquippedStack(slot).copy());
            }
        }
        this.equipStack(EquipmentSlot.MAINHAND, player.getMainHandStack().copy());
        inventoryCopyCooldown = 600;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, this::movementController));
    }

    private PlayState movementController(AnimationState<MimicEntity> state) {
        if (this.isDead()) return PlayState.STOP;
        state.setAnimation(RawAnimation.begin().thenLoop(state.isMoving() ? "animation.player.walk" : "animation.player.idle"));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("CopiedPlayerName", getCopiedPlayerName());
        nbt.putBoolean("IsAggressive", isAggressive());
        nbt.putInt("VerityMessageCount", verityMessageCount);
        if (copiedPlayerUUID != null) nbt.putUuid("CopiedUUID", copiedPlayerUUID);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        String name = nbt.getString("CopiedPlayerName");
        if (name.isEmpty()) name = getRandomFallbackName();
        setCopiedPlayerName(name);
        setAggressive(nbt.getBoolean("IsAggressive"));
        verityMessageCount = nbt.getInt("VerityMessageCount");
        if (nbt.contains("CopiedUUID")) copiedPlayerUUID = nbt.getUuid("CopiedUUID");
    }
}