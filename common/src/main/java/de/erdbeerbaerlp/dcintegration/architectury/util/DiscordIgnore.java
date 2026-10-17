package de.erdbeerbaerlp.dcintegration.architectury.util;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.api.DiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.PlayerLink;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Method;
import java.util.UUID;

// Discord messages from a linked account count as that player's: PandaIgnore's /ignore hides them,
// and clicking one suggests /ignore <name>. The core hands sendIngameMessage only the finished text,
// so the author is remembered here in onDiscordMessagePre (same thread, right before it).
public final class DiscordIgnore {
    private static final ThreadLocal<UUID> AUTHOR = new ThreadLocal<>();
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
        DiscordIntegration.INSTANCE.registerEventHandler(new DiscordEventHandler() {
            @Override
            public boolean onDiscordMessagePre(MessageReceivedEvent event) {
                AUTHOR.set(linkedPlayer(event.getAuthor().getId()));
                return false;
            }

            @Override
            public void onDiscordMessagePost(MessageReceivedEvent event) {
                AUTHOR.remove();
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
