package local.pixelmon.backport.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cosmetic supporter downloads must never block client startup or hold its map lock. */
@Pseudo @Mixin(targets="noobanidus.mods.lootr.repack.shoulders.data.ShoulderList", remap=false)
public abstract class LootrShoulderDownloadMixin {
    @Inject(method="load", at=@At("HEAD"), cancellable=true)
    private static void backport$skipSupporterDownload(CallbackInfo ci) {
        // The native renderer already accepts an empty supporter map. Lootr
        // container generation, player loot, persistence and networking are separate.
        ci.cancel();
    }
}
