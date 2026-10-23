package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.BattleEvolutionGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=PixelmonWrapper.class,remap=false,priority=1100)
public abstract class BattleEvolutionGuardMixin {
    @Inject(method="canMegaEvolve()Z",at=@At("HEAD"),cancellable=true)
    private void backport$ordinaryGreninjaStone(CallbackInfoReturnable<Boolean> cir){if(local.pixelmon.backport.PermanentAsh.blockedOrdinaryStone(((PixelmonWrapper)(Object)this).pokemon))cir.setReturnValue(false);}
    @Inject(method="megaEvolve",at=@At("HEAD"),cancellable=true)
    private void backport$aliveActor(CallbackInfoReturnable<Boolean> cir){
        PixelmonWrapper p=(PixelmonWrapper)(Object)this;
        if(!BattleEvolutionGuard.mayStart(p)||local.pixelmon.backport.PermanentAsh.blockedOrdinaryStone(p.pokemon)){BattleEvolutionGuard.discardChoice(p);cir.setReturnValue(false);}
    }
}
