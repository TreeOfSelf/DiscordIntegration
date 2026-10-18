package de.erdbeerbaerlp.dcintegration.architectury.mixin;

import de.erdbeerbaerlp.dcintegration.architectury.util.SkinHash;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The core builds webhook avatars from playerAvatarURL itself: fill in %skin% before it's sent
@Mixin(value = DiscordIntegration.class, remap = false)
public class WebhookSkinMixin {
    @Inject(method = "sendMessage(Ljava/lang/String;Lde/erdbeerbaerlp/dcintegration/common/util/DiscordMessage;Ljava/lang/String;Lnet/dv8tion/jda/api/entities/channel/middleman/MessageChannel;ZLjava/lang/String;)V",
            at = @At("HEAD"), cancellable = true)
    private void dcintegration$skinAvatar(String name, DiscordMessage msg, String avatarURL, MessageChannel channel, boolean isChatMessage, String uuid, CallbackInfo ci) {
        if (avatarURL == null || !avatarURL.contains("%skin%")) return;
        ci.cancel();
        ((DiscordIntegration) (Object) this).sendMessage(name, msg, SkinHash.apply(avatarURL, uuid), channel, isChatMessage, uuid);
    }
}
