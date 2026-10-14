package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.BattleBond;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;

/** One refreshable three-turn Water boost, local to a battle wrapper. */
public final class AshWaterBoost {
    private static final Map<PixelmonWrapper,Integer> UNTIL=new WeakHashMap<>();
    private AshWaterBoost(){}
    private static boolean eligible(PixelmonWrapper p){return p!=null&&p.bc!=null&&!p.bc.battleEnded&&p.isAlive()
        &&p.getSpecies().getDex()==658&&PermanentAsh.isAsh(p.getForm())&&p.pokemon.getAbility() instanceof BattleBond;}
    public static void start(PixelmonWrapper p){
        if(!eligible(p)||p.attack==null||p.usingZ||!p.attack.getMove().getAttackName().equals("Water Shuriken"))return;
        UNTIL.put(p,p.bc.battleTurn+3);
        if(!p.bc.simulateMode)p.bc.sendToAll("pixelmon_backport.battle.ash_water_boost",p.getNickname());
    }
    public static int power(PixelmonWrapper p,Attack attack,int power){
        Integer until=UNTIL.get(p);
        if(until==null||!eligible(p)||p.bc.battleTurn>=until||attack.getType()!=Element.WATER
            ||attack.getAttackCategory()==com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS)return power;
        return Math.max(1,(int)Math.floor(power*1.5));
    }
    private static void announceEnd(PixelmonWrapper p){if(p.bc!=null&&!p.bc.simulateMode&&!p.bc.battleEnded)p.bc.sendToAll("pixelmon_backport.battle.ash_water_boost_end",p.getNickname());}
    public static void clear(PixelmonWrapper p){if(UNTIL.remove(p)!=null)announceEnd(p);}
    public static void expire(BattleController battle){
        // Native endTurn increments battleTurn before returning. The attack
        // turn plus the next two turns have completed at this boundary.
        for(var it=UNTIL.entrySet().iterator();it.hasNext();){var row=it.next();PixelmonWrapper p=row.getKey();
            if(p.bc==battle&&(battle.battleTurn>=row.getValue()||!p.isAlive())){it.remove();announceEnd(p);}
        }
    }
    public static void copy(PixelmonWrapper from,PixelmonWrapper to){Integer until=UNTIL.get(from);if(until!=null)UNTIL.put(to,until);}
    public static void end(BattleController battle){UNTIL.keySet().removeIf(p->p.bc==battle);}
}
