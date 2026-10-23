package local.pixelmon.backport.mixin;
import local.pixelmon.backport.Tera;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.api.pokemon.ability.Ability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=Attack.class,remap=false)
public abstract class TeraAttackMixin {
    @Redirect(method="getTypeEffectiveness",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/Element;getTotalEffectiveness(Ljava/util/List;Lcom/pixelmonmod/pixelmon/api/pokemon/Element;Z)F"))
    private float backport$nihilFairy(java.util.List<Element> types,Element attacking,boolean inverse){
        // In inverse battles Fairy already takes Dragon damage; keep that chart.
        return Element.getTotalEffectiveness(inverse?types:local.pixelmon.backport.ZaMoves.defensiveTypes((Attack)(Object)this,types),attacking,inverse);
    }
    @Redirect(method="modifyMoveStats",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/ability/Ability;modifyPowerAndAccuracyUser(IILcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/attacks/Attack;)[I"))
    private int[] backport$ashBasePower(Ability ability,int power,int accuracy,PixelmonWrapper user,PixelmonWrapper target,Attack attack){
        if(local.pixelmon.backport.AshWaterBoost.eligible(user)&&!user.usingZ&&"Water Shuriken".equals(attack.getMove().getAttackName()))power=20;
        return ability.modifyPowerAndAccuracyUser(power,accuracy,user,target,attack);
    }
    @Inject(method="getTypeEffectiveness",at=@At("RETURN"),cancellable=true)
    private void backport$burst(PixelmonWrapper user,PixelmonWrapper target,CallbackInfoReturnable<Double> cir){Attack move=(Attack)(Object)this;cir.setReturnValue(local.pixelmon.backport.BurstTerrain.effectiveness(user,target,move,local.pixelmon.backport.LightningOath.effectiveness(user,target,move,cir.getReturnValue())));}
    @Redirect(method="doDamageCalc",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/ability/Ability;modifyStab(D)D"))
    private double backport$stab(Ability ability,double nativeStab,PixelmonWrapper user,PixelmonWrapper target,double crit){return Tera.stab(user,((Attack)(Object)this).getType(),ability.modifyStab(nativeStab));}
    @Inject(method="doDamageCalc",at=@At("RETURN"))
    private void backport$recordDamage(PixelmonWrapper user,PixelmonWrapper target,double crit,CallbackInfoReturnable<Integer> cir){if(cir.getReturnValue()>0)Tera.damageCalculated(user,(Attack)(Object)this);}
    @Redirect(method="getTypeEffectiveness",at=@At(value="NEW",target="com/pixelmonmod/pixelmon/api/events/battles/AttackEvent$TypeEffectiveness"))
    private com.pixelmonmod.pixelmon.api.events.battles.AttackEvent.TypeEffectiveness backport$stellar(com.pixelmonmod.pixelmon.battles.controller.BattleController bc,PixelmonWrapper user,PixelmonWrapper target,double value){
        if(Tera.active(user) && ((Attack)(Object)this).getType()==Element.MYSTERY)value=Tera.active(target)?2.0:1.0;
        return new com.pixelmonmod.pixelmon.api.events.battles.AttackEvent.TypeEffectiveness(bc,user,target,value);
    }
    @Inject(method="modifyMoveStats",at=@At("RETURN"))
    private void backport$power(PixelmonWrapper user,PixelmonWrapper target,Ability a,Ability b,int power,CallbackInfoReturnable<int[]> cir){int[] stats=cir.getReturnValue();stats[0]=local.pixelmon.backport.AshWaterBoost.power(user,(Attack)(Object)this,Tera.floorPower(user,(Attack)(Object)this,stats[0]));}
}
