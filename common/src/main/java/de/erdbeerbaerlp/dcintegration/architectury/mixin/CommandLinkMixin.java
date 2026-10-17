package de.erdbeerbaerlp.dcintegration.architectury.mixin;

import de.erdbeerbaerlp.dcintegration.architectury.util.LinkCodes;
import de.erdbeerbaerlp.dcintegration.common.discordCommands.CommandLink;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

// Discord /link takes the 5-character code: the option is text instead of a number, and it's decoded back
@Mixin(value = CommandLink.class, remap = false)
public class CommandLinkMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lde/erdbeerbaerlp/dcintegration/common/discordCommands/CommandLink;addOption(Lnet/dv8tion/jda/api/interactions/commands/OptionType;Ljava/lang/String;Ljava/lang/String;Z)Lnet/dv8tion/jda/api/interactions/commands/build/SlashCommandData;"), index = 0)
    private OptionType dcintegration$codeIsText(OptionType type) {
        return OptionType.STRING;
    }

    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;parseInt(Ljava/lang/String;)I"))
    private int dcintegration$decodeCode(String code) {
        return LinkCodes.decode(code);
    }
}
