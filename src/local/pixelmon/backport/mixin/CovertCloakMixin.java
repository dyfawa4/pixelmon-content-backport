package local.pixelmon.backport.mixin;
import local.pixelmon.backport.ReferenceItems;
import com.pixelmonmod.pixelmon.battles.attacks.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.StatsEffect;
import com.pixelmonmod.pixelmon.battles.controller.log.AttackResult;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.SpecialAttackBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=Attack.class,remap=false)
public abstract class CovertCloakMixin {
    @Redirect(method="applyAttackEffect",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/attacks/EffectBase;applyEffect(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;)V"))
    private void backport$secondary(EffectBase e,PixelmonWrapper user,PixelmonWrapper target){local.pixelmon.backport.BattleMechanics.apply(e,user,target);}
    @Redirect(method="applyAttackEffect",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/attacks/specialAttacks/StatsEffect;applyStatEffect(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/attacks/ImmutableAttack;)Lcom/pixelmonmod/pixelmon/battles/controller/log/AttackResult;"))
    private AttackResult backport$stat(StatsEffect e,PixelmonWrapper user,PixelmonWrapper target,ImmutableAttack move){return ReferenceItems.blocksSecondary(e,user,target)?AttackResult.proceed:e.applyStatEffect(user,target,move);}
}
