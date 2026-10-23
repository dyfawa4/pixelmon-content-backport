package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.SuckerPunch;
import com.pixelmonmod.pixelmon.battles.controller.log.AttackResult;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.NpcTurnKnowledge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Native Sucker Punch skips its action condition in simulation. Restore known intent only. */
@Mixin(value=SuckerPunch.class,remap=false)
public abstract class NpcConditionalMoveMixin {
    @Inject(method="applyEffectStart",at=@At("HEAD"),cancellable=true)
    private void backport$knownAction(PixelmonWrapper user,PixelmonWrapper target,CallbackInfoReturnable<AttackResult> cir){
        if(user.bc!=null&&user.bc.simulateMode&&NpcTurnKnowledge.offensive(target)==0)cir.setReturnValue(AttackResult.failed);
    }
}
