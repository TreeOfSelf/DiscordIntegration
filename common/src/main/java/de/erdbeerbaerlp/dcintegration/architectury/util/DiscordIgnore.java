package de.erdbeerbaerlp.dcintegration.architectury.util;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.api.DiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.PlayerLink;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Method;
import java.util.UUID;

// Discord messages: /dmute hides a Discord user for one player (DiscordMutes), and a linked account's
// messages also follow PandaIgnore's /ignore. Clicking one suggests /ignore <ign> (linked) or /dmute <name>. The core hands sendIngameMessage only the finished text,
// so the author is remembered here in onDiscordMessagePre (same thread, right before it).
public final class DiscordIgnore {
    private static final ThreadLocal<UUID> AUTHOR = new ThreadLocal<>();
    // The Discord author of the message being relayed: [id, name as shown in game]
    private static final ThreadLocal<String[]> DISCORD_AUTHOR = new ThreadLocal<>();
    private static Method isIgnoring;

    static {
        try {
            isIgnoring = Class.forName("me.TreeOfSelf.PandaIgnore.PandaIgnore").getMethod("isIgnoring", MinecraftServer.class, UUID.class, UUID.class);
        } catch (ReflectiveOperationException e) {
            // PandaIgnore not installed (or too old)
        }
    }

    private DiscordIgnore() {}

    public static void register() {
        // Load the classes the link-code mixins change now, so they're applied (and checked) at startup
        LinkManager.pendingLinks.size();
        try {
            Class.forName("de.erdbeerbaerlp.dcintegration.common.minecraftCommands.LinkCommand");
            Class.forName("de.erdbeerbaerlp.dcintegration.common.discordCommands.CommandLink");
        } catch (ClassNotFoundException ignored) {
        }
        DiscordIntegration.INSTANCE.registerEventHandler(new DiscordEventHandler() {
            @Override
            public boolean onDiscordMessagePre(MessageReceivedEvent event) {
                AUTHOR.set(linkedPlayer(event.getAuthor().getId()));
                String username = event.getAuthor().getName();
                String nickname = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getEffectiveName();
                DISCORD_AUTHOR.set(new String[]{event.getAuthor().getId(), username});
                DiscordMutes.seen(event.getAuthor().getId(), username, nickname);
                return false;
            }

            @Override
            public void onDiscordMessagePost(MessageReceivedEvent event) {
                AUTHOR.remove();
                DISCORD_AUTHOR.remove();
            }
        });
    }

    private static UUID linkedPlayer(String discordID) {
        if (!LinkManager.isDiscordUserLinkedToJava(discordID)) return null;
        PlayerLink link = LinkManager.getLink(discordID, null);
        try {
            return link == null || link.mcPlayerUUID == null || link.mcPlayerUUID.isEmpty() ? null : UUID.fromString(link.mcPlayerUUID);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // [discordId, Discord username] of the Discord message being relayed right now
    public static String[] discordAuthor() {
        return DISCORD_AUTHOR.get();
    }

    // The linked player behind the Discord message being relayed right now, if any
    public static UUID author() {
        return AUTHOR.get();
    }

    public static boolean isIgnoring(MinecraftServer server, UUID viewer, UUID author) {
        if (isIgnoring == null || author == null) return false;
        try {
            return (boolean) isIgnoring.invoke(null, server, viewer, author);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
