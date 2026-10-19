package local.pixelmon.backport.mixin;
import local.pixelmon.backport.NinjaTraining;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=BattleController.class,remap=false)
public abstract class NinjaActionMixin {
    @Inject(method="update",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/controller/BattleController;isEvolving()Z"),cancellable=true)
    private void backport$bonusEvolution(CallbackInfo ci){if(NinjaTraining.evolve((BattleController)(Object)this))ci.cancel();}
    @Inject(method="update",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/controller/BattleController;takeTurn(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;)V",ordinal=0,shift=At.Shift.AFTER),cancellable=true)
    private void backport$extraAction(CallbackInfo ci){if(NinjaTraining.consumePause((BattleController)(Object)this))ci.cancel();}
    @Inject(method="takeTurn",at=@At("RETURN"))
    private void backport$actionComplete(PixelmonWrapper pokemon,CallbackInfo ci){NinjaTraining.afterAction((BattleController)(Object)this,pokemon);}
    @Inject(method="endTurn",at=@At("RETURN"))
    private void backport$clearExtra(CallbackInfo ci){NinjaTraining.end((BattleController)(Object)this);}
}
