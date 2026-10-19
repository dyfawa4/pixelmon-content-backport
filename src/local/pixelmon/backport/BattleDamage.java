package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.battles.AttackEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.*;

/** Receives the native resolved stats, event-adjusted STAB and the native RNG draw. */
public final class BattleDamage {
    private BattleDamage(){}
    public static int finish(Attack move,PixelmonWrapper user,PixelmonWrapper target,int level,double attack,double defense,double stab,double type,double critical,double random){
        double weather=1;
        if(!move.ignoresWeather(user) && !UmbrellaHooks.holds(target)){
            Weather field=ZaAbilities.moveWeather(user,user.bc.globalStatusController.getWeather());Element element=move.getType();
            if(field instanceof Sunny)weather=element==Element.FIRE?1.5:element==Element.WATER?.5:1;
            if(field instanceof Rainy)weather=element==Element.WATER?1.5:element==Element.FIRE?.5:1;
        }
        double damage=DamageRules.calculate(level,move.movePower,attack,defense,user.targets.size()>1,weather,critical,(int)Math.round(random*100),stab,type);
        damage=DamageRules.itemResult(user.getUsableHeldItem().preProcessDamagingAttackUser(user,target,move,damage));
        damage=DamageRules.itemResult(target.getUsableHeldItem().preProcessDamagingAttackTarget(user,target,move,damage));
        AttackEvent.Damage event=new AttackEvent.Damage(user.bc,user,target,damage);
        Pixelmon.EVENT_BUS.post(event);
        // Keep external modifiers (including raid shields) at their existing
        // event boundary. Zero is still zero; the native minimum is positive-only.
        damage=event.damage;
        int result=(int)Math.min(Integer.MAX_VALUE,damage>0?Math.max(1,damage):0);
        if(result>0)Tera.damageCalculated(user,move);
        return result;
    }
}
