package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.*;
import com.pixelmonmod.pixelmon.battles.controller.log.AttackResult;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.AbilityShield;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Atomic Skill Swap and accurate failure reporting for ability-changing moves. */
@Mixin(value={ChangeAbility.class,Entrainment.class,RolePlay.class,SkillSwap.class},remap=false)
public abstract class AbilityShieldMoveMixin {
    @Inject(method="applyEffectDuring",at=@At("HEAD"),cancellable=true)
    private void backport$blocked(PixelmonWrapper user,PixelmonWrapper target,CallbackInfoReturnable<AttackResult> cir){
        PixelmonWrapper affected=(Object)this instanceof RolePlay?user:target;
        if((Object)this instanceof SkillSwap&&AbilityShield.active(user))affected=user;
        if(AbilityShield.active(affected)){AbilityShield.blocked(affected);cir.setReturnValue(AttackResult.failed);}
    }
}
