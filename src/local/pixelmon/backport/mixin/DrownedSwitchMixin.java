package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=PixelmonWrapper.class,remap=false)
public abstract class DrownedSwitchMixin {
 @Inject(method="afterSwitch",at=@At("HEAD"),require=1)
 private void backport$entryCurse(CallbackInfo ci){local.pixelmon.backport.DrownedErosion.enter((PixelmonWrapper)(Object)this);}
}
