package local.pixelmon.backport.mixin;
import local.pixelmon.backport.ZaAbilities;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.attacks.DamageTypeEnum;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.ProtectVariation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ProtectVariation.class,remap=false)
public abstract class PiercingDrillMixin {
    @Unique private static boolean backport$piercing(PixelmonWrapper user,PixelmonWrapper target,Attack attack){
        if(user==target||attack==null||!(user.getBattleAbility() instanceof ZaAbilities.PiercingDrill)||!attack.getMove().getMakesContact()||!Attack.dealsDamage(attack))return false;
        for(var effect:attack.getMove().effects)if(effect instanceof com.pixelmonmod.pixelmon.battles.status.Vanish||effect instanceof com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.Feint||effect instanceof com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.HyperDrill)return false;
        return true;
    }
    @Inject(method="stopsIncomingAttack",at=@At("RETURN"),cancellable=true)
    private void backport$pierce(PixelmonWrapper target,PixelmonWrapper user,CallbackInfoReturnable<Boolean> cir){
        if(!((Object)this instanceof com.pixelmonmod.pixelmon.battles.status.MaxGuard)&&cir.getReturnValue()&&backport$piercing(user,target,user.attack)){
            // Native shield retaliation still applies; only its damage block is bypassed.
            ((ProtectVariation)(Object)this).stopsIncomingAttackMessage(target,user);cir.setReturnValue(false);
        }
    }
    @Inject(method="modifyDamageIncludeFixed",at=@At("RETURN"),cancellable=true)
    private void backport$quarter(int damage,PixelmonWrapper user,PixelmonWrapper target,Attack attack,DamageTypeEnum kind,CallbackInfoReturnable<Integer> cir){
        if(!((Object)this instanceof com.pixelmonmod.pixelmon.battles.status.MaxGuard)&&!user.usingZ&&user.isDynamax<=0&&backport$piercing(user,target,attack))cir.setReturnValue((int)Math.ceil(cir.getReturnValue()/4.0));
    }
}
