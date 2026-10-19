package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.api.pokemon.ability.AbstractAbility;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Ate;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.BeastBoost;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Levitate;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.*;

/** Additional abilities verified against the official Japanese Pokedex, 2026-10-18. */
public final class ZaAbilities {
    private ZaAbilities(){}
    public static class Dragonize extends Ate {public Dragonize(){super(Element.DRAGON);}}
    public static class MegaSol extends AbstractAbility {}
    public static class PiercingDrill extends AbstractAbility {}
    public static class AuraFortify extends AbstractAbility {
        @Override public int modifyDamageTarget(int damage,PixelmonWrapper user,PixelmonWrapper target,Attack attack){return attack.getMove().getMakesContact()?damage/2:damage;}
    }
    public static class Updraft extends Levitate {
        @Override public void tookDamageUser(int damage,PixelmonWrapper user,PixelmonWrapper target,Attack attack){new BeastBoost().tookDamageUser(damage,user,target,attack);}
    }
    public static class FlamingMane extends AbstractAbility {
        @Override public int[] modifyPowerAndAccuracyUser(int power,int accuracy,PixelmonWrapper user,PixelmonWrapper target,Attack attack){return new int[]{attack.getType()==Element.FIRE?(int)(power*1.5):power,accuracy};}
    }
    public static class SpicySpray extends AbstractAbility {
        @Override public void tookDamageTarget(int damage,PixelmonWrapper user,PixelmonWrapper target,Attack attack){
            if(damage>0&&user!=target&&user.isAlive()&&Burn.burn(target,user,null,false))user.bc.sendToAll("pixelmon_backport.ability.spicy_spray",target.getNickname(),user.getNickname());
        }
    }
    public static boolean personalSun(PixelmonWrapper p){return p!=null&&p.getBattleAbility() instanceof MegaSol;}
    public static Weather moveWeather(PixelmonWrapper p,Weather actual){return personalSun(p)?new Sunny(-1):actual;}
}
