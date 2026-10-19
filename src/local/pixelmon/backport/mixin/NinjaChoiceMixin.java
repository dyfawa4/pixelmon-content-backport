package local.pixelmon.backport.mixin;
import local.pixelmon.backport.NinjaTraining;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=PixelmonWrapper.class,remap=false)
public abstract class NinjaChoiceMixin {
    @Redirect(method="turnTick",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/status/StatusBase;applyRepeatedEffect(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;)V"))
    private void backport$personalClock(com.pixelmonmod.pixelmon.battles.status.StatusBase status,PixelmonWrapper p){if(!NinjaTraining.personalTick(p)||!status.isTeamStatus())status.applyRepeatedEffect(p);}
    @Inject(method="canAttackThisTurn",at=@At("HEAD"),cancellable=true)
    private void backport$legalTraining(Attack attack,CallbackInfoReturnable<Boolean> cir){PixelmonWrapper p=(PixelmonWrapper)(Object)this;if(NinjaTraining.move(attack)&&p.isDynamax==0&&!NinjaTraining.allowed(p))cir.setReturnValue(false);}
}
