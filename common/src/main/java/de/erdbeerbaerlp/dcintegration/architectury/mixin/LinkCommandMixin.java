package de.erdbeerbaerlp.dcintegration.architectury.mixin;

import de.erdbeerbaerlp.dcintegration.architectury.util.LinkCodes;
import de.erdbeerbaerlp.dcintegration.common.minecraftCommands.LinkCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// /discord link shows the code as 5 characters instead of the number (both the Java and Bedrock message)
@Mixin(value = LinkCommand.class, remap = false)
public class LinkCommandMixin {
    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Ljava/lang/StringBuilder;append(I)Ljava/lang/StringBuilder;"))
    private StringBuilder dcintegration$showCode(StringBuilder builder, int number) {
        return builder.append(LinkCodes.encode(number));
    }
}
