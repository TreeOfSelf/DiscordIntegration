package de.erdbeerbaerlp.dcintegration.architectury.util;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.api.DiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;


// Discord messages: /ignore @<discord username> is the only way to hide a Discord user (DiscordMutes); an in-game /ignore
// of a linked player doesn't touch their Discord messages. Clicking the name suggests /ignore @<discord username>. The core hands sendIngameMessage only the finished text,
// so the author is remembered here in onDiscordMessagePre (same thread, right before it).
public final class DiscordIgnore {
    // The Discord author of the message being relayed: [id, name as shown in game]
    private static final ThreadLocal<String[]> DISCORD_AUTHOR = new ThreadLocal<>();

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
                String username = event.getAuthor().getName();
                String nickname = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getEffectiveName();
                DISCORD_AUTHOR.set(new String[]{event.getAuthor().getId(), username});
                DiscordMutes.seen(event.getAuthor().getId(), username, nickname);
                return false;
            }

            @Override
            public void onDiscordMessagePost(MessageReceivedEvent event) {
                DISCORD_AUTHOR.remove();
            }
        });
    }

    // [discordId, Discord username] of the Discord message being relayed right now
    public static String[] discordAuthor() {
        return DISCORD_AUTHOR.get();
    }
}
