package net.dalerd.backroomsbounded.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class BackroomsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "backroomsbounded.json";
    private static BackroomsConfig INSTANCE;

    @SerializedName("bacterium_max_count")
    public int bacteriumMaxCount = 1;

    @SerializedName("bacterium_walk_speed")
    public double bacteriumWalkSpeed = 0.22;

    @SerializedName("bacterium_run_speed")
    public double bacteriumRunSpeed = 0.29;

    @SerializedName("bacterium_sprint_speed")
    public double bacteriumSprintSpeed = 0.37;

    @SerializedName("bacterium_damage_easy_normal")
    public float bacteriumDamageEasyNormal = 8.0f;

    @SerializedName("bacterium_damage_hard")
    public float bacteriumDamageHard = 9.0f;

    @SerializedName("bacterium_grab_escape_clicks")
    public int bacteriumGrabEscapeClicks = 3;

    @SerializedName("bacterium_grab_duration_seconds")
    public int bacteriumGrabDurationSeconds = 3;

    @SerializedName("bacterium_grab_chance")
    public float bacteriumGrabChance = 0.30f;

    @SerializedName("armor_diamond_break_minutes")
    public int armorDiamondBreakMinutes = 8;

    @SerializedName("armor_netherite_break_minutes")
    public int armorNetheriteBreakMinutes = 10;

    @SerializedName("armor_mending_multiplier")
    public int armorMendingMultiplier = 4;

    @SerializedName("mimic_max_count")
    public int mimicMaxCount = 3;

    @SerializedName("mimic_spawn_cooldown_minutes")
    public int mimicSpawnCooldownMinutes = 3;

    @SerializedName("mimic_force_spawn_minutes")
    public int mimicForceSpawnMinutes = 8;

    @SerializedName("glitch_escape_chance")
    public float glitchEscapeChance = 0.80f;

    @SerializedName("glitch_enter_chance")
    public float glitchEnterChance = 0.35f;

    @SerializedName("panic_darkness_threshold")
    public int panicDarknessThreshold = 400;

    @SerializedName("shroom_detection_range")
    public int shroomDetectionRange = 20;

    public static BackroomsConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    private static BackroomsConfig load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        File configFile = configPath.toFile();

        if (configFile.exists()) {
            try (Reader reader = new FileReader(configFile)) {
                return GSON.fromJson(reader, BackroomsConfig.class);
            } catch (IOException e) {
                System.err.println("[BackroomsBounded] Failed to load config, using defaults: " + e.getMessage());
            }
        }

        // Create default config
        BackroomsConfig config = new BackroomsConfig();
        config.save();
        return config;
    }

    public void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        try (Writer writer = new FileWriter(configPath.toFile())) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            System.err.println("[BackroomsBounded] Failed to save config: " + e.getMessage());
        }
    }
}