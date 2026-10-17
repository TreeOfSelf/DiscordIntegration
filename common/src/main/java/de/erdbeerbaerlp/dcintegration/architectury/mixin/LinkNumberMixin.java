package de.erdbeerbaerlp.dcintegration.architectury.mixin;

import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.security.SecureRandom;
import java.util.Random;

// Link codes were Random.nextInt(99999), guessable with /link spam while someone's code is pending.
// 9 digits from SecureRandom instead (still fits /link's int parsing).
@Mixin(value = LinkManager.class, remap = false)
public class LinkNumberMixin {
    private static final SecureRandom SECURE = new SecureRandom();

    @Redirect(method = "genLinkNumber(Ljava/util/UUID;Ljava/util/HashMap;)I", at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I"))
    private static int dcintegration$secureLinkNumber(Random random, int bound) {
        return 100_000_000 + SECURE.nextInt(900_000_000);
    }
}
