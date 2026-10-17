package de.erdbeerbaerlp.dcintegration.architectury.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.erdbeerbaerlp.dcintegration.architectury.util.DiscordMutes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

// /dmute <discord username>, /dunmute <discord username>, /dmutelist: hide a Discord user's messages in game, no linking needed
public final class DiscordMuteCommands {
    private DiscordMuteCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dmute")
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(DiscordMutes.recentNames(), b))
                .executes(DiscordMuteCommands::mute)));
        dispatcher.register(Commands.literal("dunmute")
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(DiscordMutes.muted(ctx.getSource().getPlayerOrException().getUUID()).values(), b))
                .executes(DiscordMuteCommands::unmute)));
        dispatcher.register(Commands.literal("dmutelist").executes(DiscordMuteCommands::list));
    }

    private static int mute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");
        String[] user = DiscordMutes.find(player.getUUID(), name);
        if (user == null) {
            ctx.getSource().sendFailure(Component.literal("No recent Discord message from " + name + ". Click one of their messages to mute them."));
            return 0;
        }
        boolean added = DiscordMutes.mute(player.getUUID(), user[0], user[1]);
        player.sendSystemMessage(Component.literal(added ? "Muted " + user[1] + " from Discord. /dunmute " + user[1] + " to undo." : user[1] + " is already muted.").withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int unmute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");
        String[] user = DiscordMutes.find(player.getUUID(), name);
        if (user == null || !DiscordMutes.unmute(player.getUUID(), user[0])) {
            ctx.getSource().sendFailure(Component.literal(name + " isn't muted."));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Unmuted " + user[1] + ".").withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Map<String, String> muted = DiscordMutes.muted(player.getUUID());
        if (muted.isEmpty()) {
            player.sendSystemMessage(Component.literal("You haven't muted anyone from Discord.").withStyle(ChatFormatting.GRAY));
            return 1;
        }
        player.sendSystemMessage(Component.literal("Muted from Discord: " + String.join(", ", muted.values())).withStyle(ChatFormatting.GRAY));
        return 1;
    }
}
