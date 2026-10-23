package local.pixelmon.backport.mixin;
import local.pixelmon.backport.client.TitleMessages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="net.minecraft.client.util.Splashes",remap=false)
public abstract class TitleSplashMixin {
    @Inject(method="func_215276_a",at=@At("HEAD"),cancellable=true)
    private void backport$authorSplash(CallbackInfoReturnable<String> cir){cir.setReturnValue(TitleMessages.next());}
}
