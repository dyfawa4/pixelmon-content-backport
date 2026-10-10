package local.pixelmon.backport.mixin;
import local.pixelmon.backport.ZaMoves;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=PixelmonWrapper.class,remap=false)
public abstract class ZaMegaMixin {
    @Inject(method="megaEvolve",at=@At("RETURN"))
    private void backport$zaMove(CallbackInfoReturnable<Boolean> cir){PixelmonWrapper p=(PixelmonWrapper)(Object)this;if(cir.getReturnValue() && p.isMega)ZaMoves.mega(p);}
    @Inject(method="resetBattleEvolution",at=@At("HEAD"))
    private void backport$restoreMove(CallbackInfo ci){ZaMoves.end((PixelmonWrapper)(Object)this);}
}
