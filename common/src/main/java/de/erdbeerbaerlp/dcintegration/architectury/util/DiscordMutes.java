package de.erdbeerbaerlp.dcintegration.architectury.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Per player: Discord users whose messages they don't want to see in game, no linking needed.
// PandaIgnore's /ignore @<discord username> (and /unignore, /ignorelist) call these by reflection.
// Saved in DiscordIntegration-Data/DiscordMutes.json as { playerUuid: { discordId: discordUsername } }.
// Also remembers the names of recent Discord authors so they can be typed (and tab-completed) by name.
public final class DiscordMutes {
    private static final Path FILE = Path.of("DiscordIntegration-Data", "DiscordMutes.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int RECENT_MAX = 200;

    private static Map<UUID, Map<String, String>> mutes = new ConcurrentHashMap<>();
    // lowercased name -> [discordId, name as shown], most recent last
    private static final Map<String, String[]> recent = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String[]> eldest) {
            return size() > RECENT_MAX;
        }
    });

    private DiscordMutes() {}

    public static void load() {
        try {
            if (Files.isRegularFile(FILE)) {
                Map<UUID, Map<String, String>> read = GSON.fromJson(Files.readString(FILE), new TypeToken<Map<UUID, Map<String, String>>>() {}.getType());
                if (read != null) {
                    Map<UUID, Map<String, String>> m = new ConcurrentHashMap<>();
                    read.forEach((k, v) -> m.put(k, new ConcurrentHashMap<>(v)));
                    mutes = m;
                }
            }
        } catch (Exception e) {
            DiscordIntegration.LOGGER.error("Could not read " + FILE, e);
        }
    }

    private static synchronized void save() {
        try {
            Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(mutes));
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            DiscordIntegration.LOGGER.error("Could not save " + FILE, e);
        }
    }

    // Muting is by Discord username (unique, no spaces); the server nickname shown in game finds them too
    public static void seen(String discordId, String username, String nickname) {
        String[] entry = {discordId, username};
        recent.put(nickname.toLowerCase(Locale.ROOT), entry);
        recent.put(username.toLowerCase(Locale.ROOT), entry);
    }

    // [discordId, name] for a recent author, or for someone this player already muted
    public static String[] find(UUID player, String name) {
        String key = name.strip().toLowerCase(Locale.ROOT);
        String[] r = recent.get(key);
        if (r != null) return r;
        for (Map.Entry<String, String> e : mutes.getOrDefault(player, Map.of()).entrySet())
            if (e.getValue().toLowerCase(Locale.ROOT).equals(key)) return new String[]{e.getKey(), e.getValue()};
        return null;
    }

    public static Iterable<String> recentNames() {
        synchronized (recent) {
            return recent.values().stream().map(v -> v[1]).distinct().toList();
        }
    }

    public static boolean mute(UUID player, String discordId, String name) {
        boolean added = mutes.computeIfAbsent(player, p -> new ConcurrentHashMap<>()).put(discordId, name) == null;
        save();
        return added;
    }

    public static boolean unmute(UUID player, String discordId) {
        Map<String, String> m = mutes.get(player);
        boolean removed = m != null && m.remove(discordId) != null;
        if (removed) save();
        return removed;
    }

    public static Map<String, String> muted(UUID player) {
        return mutes.getOrDefault(player, Map.of());
    }

    public static boolean isMuted(UUID player, String discordId) {
        return discordId != null && mutes.getOrDefault(player, Map.of()).containsKey(discordId);
    }
}
