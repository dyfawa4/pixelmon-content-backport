package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.Tera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Enforce the same active-Pokemon restriction at the authoritative endpoints. */
@Mixin(value=PixelmonWrapper.class,remap=false)
public abstract class TeraNativeGimmickGuardMixin {
 @Inject(method={"canMegaEvolve()Z","canUltraBurst()Z","canDynamax()Z","megaEvolve()Z","registerZMove()Z"},at=@At("HEAD"),cancellable=true)
 private void backport$active(CallbackInfoReturnable<Boolean> cir){if(Tera.active((PixelmonWrapper)(Object)this))cir.setReturnValue(false);}
 @Inject(method="dynamax(ZF)Z",at=@At("HEAD"),cancellable=true)
 private void backport$max(boolean ending,float scale,CallbackInfoReturnable<Boolean> cir){if(!ending&&Tera.active((PixelmonWrapper)(Object)this))cir.setReturnValue(false);}
}
