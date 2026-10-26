package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.log.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.AuthorChallenge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=Attack.class,remap=false)
public abstract class AuthorSelfEffectMixin {
    @Inject(method="useSelfTargetingMoves",at=@At("HEAD"),cancellable=true)
    private void backport$selfSuccess(PixelmonWrapper user,PixelmonWrapper target,MoveResults result,CallbackInfoReturnable<Boolean> ci){
        if(user==target&&AuthorChallenge.selfEffect(((Attack)(Object)this).getMove())&&!AuthorChallenge.buff(user)){
            result.result=AttackResult.failed;if(!user.bc.simulateMode)user.bc.sendToAll("pixelmon.effect.effectfailed");ci.setReturnValue(true);
        }
    }
}
