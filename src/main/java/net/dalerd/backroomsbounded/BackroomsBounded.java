package net.dalerd.backroomsbounded;

import net.dalerd.backroomsbounded.block.ModBlocks;
import net.dalerd.backroomsbounded.block.entity.ModBlockEntities;
import net.dalerd.backroomsbounded.client.LockerEffectsClient;
import net.dalerd.backroomsbounded.event.BlockGlitchHandler;

import net.dalerd.backroomsbounded.event.FroglightCorruptionHandler;
import net.dalerd.backroomsbounded.event.RandomBackroomsTeleportHandler;
import net.dalerd.backroomsbounded.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.dalerd.backroomsbounded.item.ModItemGroups;

public class BackroomsBounded implements ModInitializer {

	public static final String MOD_ID = "backroomsbounded";

	public static final Logger LOGGER =
			LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		LockerEffectsClient.register();

		ModItems.registerModItems();

		ModBlockEntities.registerBlockEntities();

		LOGGER.info("Initializing BackroomsBounded");

		ModBlocks.registerModBlocks();
		FroglightCorruptionHandler.register();

		BlockGlitchHandler.register();
		RandomBackroomsTeleportHandler.register();

		// TICK GLITCH SYSTEM
		ServerTickEvents.END_SERVER_TICK.register(this::tickServer);

		ModItemGroups.registerItemGroups();
	}

	private void tickServer(MinecraftServer server) {

		for (ServerWorld world : server.getWorlds()) {

			BlockGlitchHandler.tick(world);
		}
	}
}