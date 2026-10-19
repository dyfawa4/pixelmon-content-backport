package local.pixelmon.backport.mixin;
import local.pixelmon.backport.BattleDamage;
import com.pixelmonmod.pixelmon.api.events.battles.AttackEvent;
import com.pixelmonmod.pixelmon.api.pokemon.ability.Ability;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=Attack.class,remap=false)
public abstract class BattleDamageMixin {
    @Redirect(method="modifyMoveStats",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/status/StatusBase;modifyPowerAndAccuracyTarget(IILcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/attacks/Attack;)[I"))
    private int[] backport$deferWeather(StatusBase status,int power,int accuracy,PixelmonWrapper user,PixelmonWrapper target,Attack move){
        return status instanceof Sunny || status instanceof Rainy?new int[]{power,accuracy}:status.modifyPowerAndAccuracyTarget(power,accuracy,user,target,move);
    }
    @Inject(method="doDamageCalc",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/items/HeldItem;preProcessDamagingAttackUser(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/attacks/Attack;D)D"),locals=LocalCapture.CAPTURE_FAILHARD,cancellable=true)
    private void backport$roundedDamage(PixelmonWrapper user,PixelmonWrapper target,double crit,CallbackInfoReturnable<Integer> cir,
        Ability ability,double stab,AttackEvent.Stab event,double type,double critical,double modifier,double attack,double defense,double level,
        BattleStatsType attackStat,BattleStatsType defenseStat,double random,double oldBase,double oldDamage){
        cir.setReturnValue(BattleDamage.finish((Attack)(Object)this,user,target,(int)level,attack,defense,stab,type,critical,random));
    }
}
