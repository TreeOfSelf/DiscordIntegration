package de.erdbeerbaerlp.dcintegration.architectury.mixin;


import com.mojang.authlib.GameProfile;
import de.erdbeerbaerlp.dcintegration.architectury.DiscordIntegrationMod;
import de.erdbeerbaerlp.dcintegration.architectury.util.MessageUtilsImpl;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import de.erdbeerbaerlp.dcintegration.common.util.TextColors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

import static de.erdbeerbaerlp.dcintegration.common.DiscordIntegration.INSTANCE;

@Mixin(value = ServerGamePacketListenerImpl.class)
public class NetworkHandlerMixin {
    @Shadow
    public ServerPlayer player;

    private static UUID getActualUUID(ServerPlayer player) {
        try {
            Class<?> playerClass = player.getClass().getSuperclass();
            Class<?> impersonatorClass = Class.forName("org.ladysnake.impersonate.Impersonator");
            Object impersonator = impersonatorClass.getMethod("get", playerClass).invoke(null, player);
            boolean isImpersonating = (boolean) impersonatorClass.getMethod("isImpersonating").invoke(impersonator);
            if (isImpersonating) {
                GameProfile profile = (GameProfile) impersonatorClass.getMethod("getActualProfile").invoke(impersonator);
                return profile.getId();
            }
        } catch (Exception ignored) {}
        return player.getUUID();
    }

    private static UUID getDisplayUUID(ServerPlayer player) {
        try {
            Class<?> playerClass = player.getClass().getSuperclass();
            Class<?> impersonatorClass = Class.forName("org.ladysnake.impersonate.Impersonator");
            Object impersonator = impersonatorClass.getMethod("get", playerClass).invoke(null, player);
            boolean isImpersonating = (boolean) impersonatorClass.getMethod("isImpersonating").invoke(impersonator);
            if (isImpersonating) {
                GameProfile profile = (GameProfile) impersonatorClass.getMethod("getImpersonatedProfile").invoke(impersonator);
                if (profile.getId() != null) return profile.getId();
            }
        } catch (Exception ignored) {}
        return player.getUUID();
    }

    private static String getDisplayName(ServerPlayer player) {
        try {
            Class<?> playerClass = player.getClass().getSuperclass();
            Class<?> impersonatorClass = Class.forName("org.ladysnake.impersonate.Impersonator");
            Object impersonator = impersonatorClass.getMethod("get", playerClass).invoke(null, player);
            boolean isImpersonating = (boolean) impersonatorClass.getMethod("isImpersonating").invoke(impersonator);
            if (isImpersonating) {
                GameProfile profile = (GameProfile) impersonatorClass.getMethod("getImpersonatedProfile").invoke(impersonator);
                if (profile.getName() != null) return profile.getName();
            }
        } catch (Exception ignored) {}
        return player.getName().getString();
    }

    /**
     * Handle possible timeout
     */
    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onDisconnect(DisconnectionDetails disconnectionDetails, CallbackInfo ci) {
        final Component reason = disconnectionDetails.reason();
        if (DiscordIntegrationMod.stopped) return; //Try to fix player leave messages after stop!
        UUID actualUUID = getActualUUID(player);
        UUID displayUUID = getDisplayUUID(player);
        String displayName = getDisplayName(player);
        if(INSTANCE.getServerInterface().isPlayerVanish(actualUUID)) return;
        if (LinkManager.isPlayerLinked(actualUUID) && LinkManager.getLink(null, actualUUID).settings.hideFromDiscord) {
            return;
        }
        if (reason.equals(Component.translatable("disconnect.timeout")))
            DiscordIntegrationMod.timeouts.add(actualUUID);
        INSTANCE.callEventC((a)->a.onPlayerLeave(actualUUID));
        final String avatarURL = INSTANCE.getSkinURL().replace("%uuid%", displayUUID.toString()).replace("%uuid_dashless%", displayUUID.toString().replace("-", "")).replace("%name%", displayName).replace("%randomUUID%", UUID.randomUUID().toString());
        if (DiscordIntegration.INSTANCE != null && !DiscordIntegrationMod.timeouts.contains(actualUUID)) {
            if (!Localization.instance().playerLeave.isBlank()) {
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.playerLeaveMessages.asEmbed) {
                    if (!Configuration.instance().embedMode.playerLeaveMessages.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.playerLeaveMessages.toEmbedJson(Configuration.instance().embedMode.playerLeaveMessages.customJSON
                                .replace("%uuid%", displayUUID.toString())
                                .replace("%uuid_dashless%", displayUUID.toString().replace("-", ""))
                                .replace("%name%", displayName)
                                .replace("%randomUUID%", UUID.randomUUID().toString())
                                .replace("%avatarURL%", avatarURL)
                                .replace("%playerColor%", "" + TextColors.generateFromUUID(displayUUID).getRGB())
                        );
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                    } else {
                        final EmbedBuilder b = Configuration.instance().embedMode.playerLeaveMessages.toEmbed().setAuthor(displayName, null, avatarURL)
                                .setDescription(Localization.instance().playerLeave.replace("%player%", displayName));
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                    }
                } else
                    DiscordIntegration.INSTANCE.sendMessage(Localization.instance().playerLeave.replace("%player%", displayName),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
            }
        } else if (DiscordIntegration.INSTANCE != null && DiscordIntegrationMod.timeouts.contains(actualUUID)) {
            if (!Localization.instance().playerTimeout.isBlank()) {
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.playerLeaveMessages.asEmbed) {
                    final EmbedBuilder b = Configuration.instance().embedMode.playerLeaveMessages.toEmbed()
                            .setAuthor(displayName, null, avatarURL)
                            .setDescription(Localization.instance().playerTimeout.replace("%player%", displayName));
                    DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                } else
                    DiscordIntegration.INSTANCE.sendMessage(Localization.instance().playerTimeout.replace("%player%", displayName),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
            }
            DiscordIntegrationMod.timeouts.remove(actualUUID);
        }
    }
}
