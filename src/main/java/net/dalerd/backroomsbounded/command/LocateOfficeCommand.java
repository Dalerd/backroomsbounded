package net.dalerd.backroomsbounded.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.dalerd.backroomsbounded.world.gen.BackroomsDimension;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;

public class LocateOfficeCommand {

    private static final int OFFICE_SQUARE_SIZE = 6;
    private static final int REGION_SIZE = 64;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(
                CommandManager.literal("locateoffice")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(LocateOfficeCommand::execute)
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) {
            source.sendFeedback(() -> Text.literal("This command can only be used by a player."), false);
            return 0;
        }

        // Check if player is in the backrooms
        if (player.getWorld().getRegistryKey() != BackroomsDimension.BACKROOMS_LEVEL_KEY) {
            source.sendFeedback(() -> Text.literal("§cYou must be in the Backrooms to locate an office complex."), false);
            return 0;
        }

        BlockPos playerPos = player.getBlockPos();
        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;

        long worldSeed = player.getServerWorld().getSeed();

        // Search nearby regions for an office
        for (int searchRadius = 0; searchRadius < 10; searchRadius++) {
            for (int drx = -searchRadius; drx <= searchRadius; drx++) {
                for (int drz = -searchRadius; drz <= searchRadius; drz++) {
                    int regionX = Math.floorDiv(chunkX, REGION_SIZE) + drx;
                    int regionZ = Math.floorDiv(chunkZ, REGION_SIZE) + drz;

                    long regionSeed = ((long) regionX * 341873128712L + (long) regionZ * 132897987541L) ^ worldSeed;
                    ChunkRandom rr = new ChunkRandom(new CheckedRandom(regionSeed));

                    if (rr.nextFloat() < 0.10f) {
                        int maxOffset = REGION_SIZE - OFFICE_SQUARE_SIZE;
                        int minCX = regionX * REGION_SIZE + rr.nextInt(maxOffset + 1);
                        int minCZ = regionZ * REGION_SIZE + rr.nextInt(maxOffset + 1);

                        // Calculate center of the office square
                        int centerX = (minCX + OFFICE_SQUARE_SIZE / 2) * 16 + 8;
                        int centerZ = (minCZ + OFFICE_SQUARE_SIZE / 2) * 16 + 8;

                        int distance = (int) Math.sqrt(playerPos.getSquaredDistance(
                                new BlockPos(centerX, playerPos.getY(), centerZ)));

                        source.sendFeedback(() -> Text.literal(
                                "§aOffice Complex found! §7Center: §f" + centerX + ", ~, " + centerZ +
                                        " §7(§f" + distance + " blocks away§7)"), false);

                        return 1;
                    }
                }
            }
        }

        source.sendFeedback(() -> Text.literal("§cNo Office Complex found within 10 regions. Try exploring further."), false);
        return 0;
    }
}