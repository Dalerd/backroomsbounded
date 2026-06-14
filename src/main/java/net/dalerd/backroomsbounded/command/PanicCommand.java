package net.dalerd.backroomsbounded.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.dalerd.backroomsbounded.sanity.SanityManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class PanicCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("panic")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0, 100))
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    SanityManager.setPanic(player, amount);
                                    context.getSource().sendFeedback(() -> Text.literal("Panic set to " + amount), false);
                                    return 1;
                                })
                        )
                )
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0, 100))
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    SanityManager.addPanic(player, amount);
                                    int current = SanityManager.getPanic(player);
                                    context.getSource().sendFeedback(() -> Text.literal("Panic increased to " + current), false);
                                    return 1;
                                })
                        )
                )
                .then(CommandManager.literal("get")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            int current = SanityManager.getPanic(player);
                            context.getSource().sendFeedback(() -> Text.literal("Current panic: " + current), false);
                            return 1;
                        })
                )
                .then(CommandManager.literal("reset")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            SanityManager.setPanic(player, 0);
                            context.getSource().sendFeedback(() -> Text.literal("Panic reset to 0"), false);
                            return 1;
                        })
                )
        );
    }
}
