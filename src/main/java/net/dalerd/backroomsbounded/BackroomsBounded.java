package net.dalerd.backroomsbounded;

import net.dalerd.backroomsbounded.block.ModBlocks;
import net.dalerd.backroomsbounded.block.entity.ModBlockEntities;
import net.dalerd.backroomsbounded.command.PanicCommand;
import net.dalerd.backroomsbounded.event.*;
import net.dalerd.backroomsbounded.item.ModItems;
import net.dalerd.backroomsbounded.item.ModItemGroups;
import net.dalerd.backroomsbounded.sanity.*;
import net.dalerd.backroomsbounded.world.gen.BackroomsChunkGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackroomsBounded implements ModInitializer {

	public static final String MOD_ID = "backroomsbounded";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing BackroomsBounded");

		// Register blocks and items first
		ModBlocks.registerModBlocks();
		ModItems.registerModItems();
		ModBlockEntities.registerBlockEntities();

		// Register chunk generator
		Registry.register(
				Registries.CHUNK_GENERATOR,
				Identifier.of(MOD_ID, "backrooms_chunk_generator"),
				BackroomsChunkGenerator.CODEC
		);

		// Register event handlers
		BackroomsLootHandler.register();
		FroglightFlickerHandler.register();
		BlockGlitchHandler.register();
		RandomBackroomsTeleportHandler.register();

		// Register sanity system
		SanityEvents.register();
		SanityChatMessages.register();
		SanityFoodHandler.register();
		SanityDesktopFiles.register();
		SilhouetteSpawner.register();

		// Register panic command for testing
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			PanicCommand.register(dispatcher, registryAccess, environment);
		});

		// Register server tick events
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			tickServer(server);
		});

		// Register item groups last
		ModItemGroups.registerItemGroups();

		LOGGER.info("BackroomsBounded initialized successfully");
	}

	private void tickServer(MinecraftServer server) {
		for (ServerWorld world : server.getWorlds()) {
			BlockGlitchHandler.tick(world);
		}
	}
}
