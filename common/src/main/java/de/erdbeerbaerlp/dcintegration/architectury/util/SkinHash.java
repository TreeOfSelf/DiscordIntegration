package de.erdbeerbaerlp.dcintegration.architectury.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static de.erdbeerbaerlp.dcintegration.architectury.DiscordIntegrationMod.server;

// %skin% in avatar URLs and embed JSON: the player's Mojang texture hash (from the skin they joined with), so a URL
// like https://hardcoreanarchy.gay/api/skin/face/%skin%/32.png is the same for one skin (Discord caches it) and new
// as soon as the skin changes. Falls back to the UUID (the site then looks the current skin up itself).
public final class SkinHash {
    private static final Map<UUID, String> KNOWN = new ConcurrentHashMap<>();

    private SkinHash() {}

    public static String of(UUID uuid) {
        final ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            final String hash = fromPlayer(player);
            if (hash != null) KNOWN.put(uuid, hash);
        }
        return KNOWN.getOrDefault(uuid, uuid.toString());
    }

    public static String apply(String text, String uuid) {
        if (text == null || !text.contains("%skin%")) return text;
        try {
            return text.replace("%skin%", of(UUID.fromString(uuid)));
        } catch (IllegalArgumentException e) {
            return text;
        }
    }

    private static String fromPlayer(ServerPlayer player) {
        try {
            for (Property property : player.getGameProfile().properties().get("textures")) {
                final JsonObject textures = JsonParser.parseString(new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8))
                        .getAsJsonObject().getAsJsonObject("textures");
                if (textures == null || !textures.has("SKIN")) return null;
                final String url = textures.getAsJsonObject("SKIN").get("url").getAsString();
                final String hash = url.substring(url.lastIndexOf('/') + 1);
                return hash.matches("[0-9a-f]{40,64}") ? hash : null;
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }
}
