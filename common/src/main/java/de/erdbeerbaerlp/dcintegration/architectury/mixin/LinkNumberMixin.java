package de.erdbeerbaerlp.dcintegration.architectury.mixin;

import de.erdbeerbaerlp.dcintegration.architectury.util.LinkCodes;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

// Link codes were Random.nextInt(99999), guessable with /link spam while someone's code is pending.
// Now a SecureRandom number shown as a 5-character code (LinkCodes).
@Mixin(value = LinkManager.class, remap = false)
public class LinkNumberMixin {
    @Redirect(method = "genLinkNumber(Ljava/util/UUID;Ljava/util/HashMap;)I", at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I"))
    private static int dcintegration$secureLinkNumber(Random random, int bound) {
        return LinkCodes.next();
    }
}
