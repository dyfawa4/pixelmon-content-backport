package local.pixelmon.backport.mixin;
import com.pixelmonmod.tcg.listener.TCGServerEventListeners;
import com.pixelmonmod.pixelmon.api.events.BeatWildPixelmonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=TCGServerEventListeners.class,remap=false)
public abstract class TcgEssenceMixin {
    @Inject(method="onKnockoutEvent",at=@At("HEAD"),cancellable=true)
    private void backport$removeEssence(BeatWildPixelmonEvent event,CallbackInfo ci){ci.cancel();}
}
