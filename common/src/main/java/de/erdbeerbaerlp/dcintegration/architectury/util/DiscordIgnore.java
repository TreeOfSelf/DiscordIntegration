package de.erdbeerbaerlp.dcintegration.architectury.util;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.api.DiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


// Discord messages: /ignore @<discord username> is the only way to hide a Discord user (DiscordMutes); an in-game /ignore
// of a linked player doesn't touch their Discord messages. Clicking the name suggests /ignore @<discord username>. The core hands sendIngameMessage only the finished text,
// so the author is remembered here in onDiscordMessagePre (same thread, right before it).
public final class DiscordIgnore {
    // The Discord author of the message being relayed: [id, name as shown in game]
    private static final ThreadLocal<String[]> DISCORD_AUTHOR = new ThreadLocal<>();
    // Mentions in the message being relayed, as Discord shows them in the text ("@name", "@role", "@everyone")
    private static final ThreadLocal<List<String>> MENTIONS = new ThreadLocal<>();

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
                final Mentions m = event.getMessage().getMentions();
                final List<String> mentions = new ArrayList<>();
                for (Member member : m.getMembers()) mentions.add("@" + member.getEffectiveName());
                for (User user : m.getUsers()) mentions.add("@" + user.getName());
                for (Role role : m.getRoles()) mentions.add("@" + role.getName());
                if (m.mentionsEveryone()) {
                    mentions.add("@everyone");
                    mentions.add("@here");
                }
                // Longest first, so "@Bob Smith" wins over "@Bob"
                mentions.sort(Comparator.comparingInt(String::length).reversed());
                MENTIONS.set(mentions);
                return false;
            }

            @Override
            public void onDiscordMessagePost(MessageReceivedEvent event) {
                DISCORD_AUTHOR.remove();
                MENTIONS.remove();
            }
        });
    }

    // [discordId, Discord username] of the Discord message being relayed right now
    public static String[] discordAuthor() {
        return DISCORD_AUTHOR.get();
    }

    // Mention texts of the Discord message being relayed right now (empty if none)
    public static List<String> mentions() {
        final List<String> m = MENTIONS.get();
        return m == null ? List.of() : m;
    }
}
