package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.Haze;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.log.AttackResult;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;

/** Respond to the actual boosted attacker, not a species or a predetermined counter. */
public final class NpcMomentum {
    private NpcMomentum(){}
    public static int pressure(PixelmonWrapper foe){
        boolean physical=false,special=false;
        for(var attack:foe.getMoveset())if(attack.pp>0&&!attack.getDisabled()){
            physical|=attack.getAttackCategory()==AttackCategory.PHYSICAL;
            special|=attack.getAttackCategory()==AttackCategory.SPECIAL;
        }
        var stats=foe.getBattleStats();
        int offence=Math.max(physical?Math.max(0,stats.getStage(BattleStatsType.ATTACK)):0,
            special?Math.max(0,stats.getStage(BattleStatsType.SPECIAL_ATTACK)):0);
        return Math.max(0,stats.getEvasionStage())+Math.max(0,stats.getStage(BattleStatsType.DEFENSE))+Math.max(0,stats.getStage(BattleStatsType.SPECIAL_DEFENSE))+(offence==0?0:offence+Math.max(0,stats.getStage(BattleStatsType.SPEED)));
    }
    public static boolean active(PixelmonWrapper own){
        return own.getOpponentPokemon().stream().anyMatch(p->p.isAlive()&&pressure(p)>0);
    }
    /** Status simulation already supplies legality (including Taunt, Substitute and immunity). */
    public static void weight(PixelmonWrapper own,List<MoveChoice> choices){
        if(!active(own)||own.getOpponentPokemon().size()!=1)return;
        var foe=own.getOpponentPokemon().get(0);
        for(var choice:choices){
            if(choice.attack==null||choice.attack.pp<=0||choice.attack.getDisabled()
                ||choice.result==null||choice.result.result==AttackResult.failed)continue;
            boolean clears=choice.attack.getMove().effects.stream().anyMatch(e->e instanceof Haze&&!((Haze)e).onlyLowered);
            if(!clears)continue;
            boolean global=choice.attack.getMove().getAttackName().equals("Haze");
            if(!global&&(!choice.targets.contains(foe)||choice.isOffensiveMove()&&choice.result.damage<=0))continue;
            // Haze also erases our own boosts. Do not spend a turn to lose more than we remove.
            int gain=foe.getBattleStats().getSumIncreases()-(global?own.getBattleStats().getSumIncreases():0);
            if(gain<=0)continue;
            var threats=NpcTactics.threats(own,choice);
            if(MoveChoice.canOutspeed(threats,own,choice.createList())&&NpcTactics.incoming(own,threats)>=own.getHealth())continue;
            // A fast reset must actually reduce the next hit enough to survive it.
            var cleanFoe=NpcSimulationCopy.of(foe);cleanFoe.getBattleStats().clearBattleStats(true);
            PixelmonWrapper cleanOwn=own;
            if(global){cleanOwn=NpcSimulationCopy.of(own);cleanOwn.getBattleStats().clearBattleStats(true);}
            double after=0;
            for(var response:threats)if(response.attack!=null&&response.user==foe){
                var hit=NpcIntentResponse.raw(own.getBattleAI(),cleanFoe,response.attack,cleanOwn);
                if(hit.result!=null&&hit.result.result!=AttackResult.failed)after=Math.max(after,Math.max(hit.result.damage,hit.result.fullDamage));
            }
            if(after*1.1>=own.getHealth())continue;
            choice.raiseTier(2);choice.weight+=Math.min(150,50+gain*20);
        }
    }
    /** Evaluate the turn spent switching AND the following action; an immunity is not assumed from typing. */
    public static boolean safeEntry(PixelmonWrapper incoming,double hit,MoveChoice reply,List<MoveChoice> threats){
        if(reply==null||reply.attack==null||reply.tier<=0||reply.result==null||reply.result.result==AttackResult.failed
            ||reply.attack.pp<=0||reply.attack.getDisabled()||!Float.isFinite(reply.weight)||reply.weight<=0)return false;
        if(hit*1.1>=incoming.getHealth())return false;
        // Even a strong reserve is not a counter if it faints before its next move.
        boolean actsFirst=!MoveChoice.canOutspeed(threats,incoming,reply.createList());
        boolean stops=NpcTactics.finishFirst(incoming,reply,threats)
            ||reply.attack.getMove().effects.stream().anyMatch(e->e instanceof Haze&&!((Haze)e).onlyLowered);
        return actsFirst&&stops||hit*2.1<incoming.getHealth();
    }
    public static float switchBonus(PixelmonWrapper outgoing,PixelmonWrapper reserve,double before,double after,MoveChoice reply,List<MoveChoice> threats){
        if(!active(outgoing)||before<outgoing.getHealth()*.75||!safeEntry(reserve,after,reply,threats))return 0;
        double improvement=before/Math.max(1,outgoing.getHealth())-after/Math.max(1,reserve.getHealth());
        if(improvement<.5)return 0;
        return (float)Math.min(120,50+50*improvement);
    }
}
