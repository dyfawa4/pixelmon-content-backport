package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.log.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.*;

/** Compare staying with voluntary switches on a committed guard turn; forced/rescue switches are retained. */
public final class NpcGuardControl {
    private NpcGuardControl(){}
    private static boolean usable(PixelmonWrapper own,MoveChoice c){
        return c!=null&&c.user==own&&c.attack!=null&&c.attack.pp>0&&!c.attack.getDisabled()
            &&(own.choiceLocked==null||own.choiceLocked.getMove().equals(c.attack.getMove()));
    }
    private static PixelmonWrapper clean(PixelmonWrapper foe){
        var copy=NpcSimulationCopy.of(foe);copy.getStatuses().removeIf(s->s instanceof ProtectVariation);
        copy.attack=copy.getMoveset().stream().filter(a->a.pp>0&&a.getAttackCategory()!=AttackCategory.STATUS).findFirst().orElse(null);
        copy.selectedAttack=copy.attack;return copy;
    }
    private static double damage(PixelmonWrapper own,PixelmonWrapper foe,Attack move){
        var c=NpcIntentResponse.raw(own.getBattleAI(),own,move,foe);
        return c.result==null||c.result.result==AttackResult.failed?0:Math.max(c.result.damage,c.result.fullDamage);
    }
    private static double future(PixelmonWrapper mon,PixelmonWrapper foe){
        double best=0;for(var a:mon.getMoveset())if(a.pp>0&&!a.getDisabled()&&a.getAttackCategory()!=AttackCategory.STATUS)best=Math.max(best,damage(mon,foe,a));return best;
    }
    public static MoveChoice refine(PixelmonWrapper own,MoveChoice original){
        if(original==null||original.switchPokemon==null||original.tier>=3||own.bc==null||own.bc.simulateMode
            ||!NpcTurnKnowledge.present(own.bc)||!NpcTurnKnowledge.enabled(own)||own.getOpponentPokemon().size()!=1)return original;
        var foe=own.getOpponentPokemon().get(0);var guard=NpcSetupControl.submitted(foe);
        if(guard==null||guard.getMove().effects.stream().noneMatch(e->e instanceof ProtectVariation))return original;
        // Trading PP only has value if an actual move can be used. Preserve a
        // native switch that escapes sleep, full paralysis or confusion self-hit.
        if(own.getMoveset().stream().noneMatch(a->a.pp>0&&!a.getDisabled()
            &&(own.choiceLocked==null||own.choiceLocked.getMove().equals(a.getMove()))&&NpcActionAvailability.canAct(own,a)))return original;
        var reserve=own.getParticipant().getPokemonFromParty(original.switchPokemon);if(reserve==null||!reserve.isAlive())return original;
        // Perish, escalating toxic and other clocks can make leaving necessary even on a safe guard turn.
        for(var status:own.getStatuses())if(status.type==StatusType.Perish||Set.of("PoisonBad","PoisonBadly","Curse").contains(status.getClass().getSimpleName()))return original;
        double incoming=NpcTactics.incoming(own,NpcTactics.threats(own));if(incoming*1.1>=own.getHealth())return original;
        var arrival=NpcSimulationCopy.of(reserve);arrival.onBattlefield=true;
        // Bench wrappers do not yet have active battle modifiers. Preview the
        // detached arrival with native stats before comparing its next attack.
        boolean simulation=own.bc.simulateMode;own.bc.simulateMode=true;
        try{own.bc.modifyStats(arrival);}finally{own.bc.simulateMode=simulation;}
        double entry=0;
        for(var h:own.getEntryHazards())if(!h.isImmune(arrival))entry+=Math.max(0,h.getDamage(arrival));
        String ability=own.getBattleAbility().getClass().getSimpleName();
        if(ability.equals("Regenerator")&&Math.min(own.getHealthDeficit(),own.getMaxHealth()/3d)>entry+own.getMaxHealth()*.1)return original;
        if(ability.equals("NaturalCure")&&own.hasNonVolatileStatus()&&entry<own.getMaxHealth()*.33)return original;
        var target=clean(foe);boolean previous=NpcTurnKnowledge.future(own.bc,true);double current,next;
        try{current=future(own,target);next=future(arrival,target);}finally{NpcTurnKnowledge.future(own.bc,previous);}
        // Real coverage/pressure improvement is productive; moving solely because
        // this turn is protected is not. Entry damage and sacrificed boosts matter.
        if(arrival.getHealth()>entry&&(current<=0&&next>0
            ||current<foe.getMaxHealth()*.22&&next>=foe.getMaxHealth()*.45&&next>current*1.7&&own.getBattleStats().getSumIncreases()==0))return original;
        MoveChoice stay=null;
        for(var c:((local.pixelmon.backport.mixin.NpcTacticalChoicesAccess)own.getBattleAI()).backport$weightedChoices(own))if(usable(own,c)&&c.result!=null&&c.result.result!=AttackResult.failed&&c.weight>0&&Float.isFinite(c.weight))
            if(stay==null||c.tier>stay.tier||c.tier==stay.tier&&c.weight>stay.weight)stay=c;
        if(stay==null){
            int pp=-1;
            for(var c:((local.pixelmon.backport.mixin.NpcDamagePreviewAccess)own.getBattleAI()).backport$legalChoices(own))if(usable(own,c)&&c.isOffensiveMove()
                &&!Set.of("Explosion","Self-Destruct","Final Gambit","Misty Explosion","Mind Blown","Steel Beam","Chloroblast","High Jump Kick","Jump Kick","Axe Kick","Supercell Slam").contains(c.attack.getMove().getAttackName())&&c.attack.pp>pp){stay=c;pp=c.attack.pp;}
            if(stay!=null){
                // Spend a normal legal high-PP move into the successful protection,
                // not a Z/Dynamax resource. Execution still uses fresh native results.
                stay.result=new MoveResults(stay.targets.isEmpty()?foe:stay.targets.get(0));stay.result.result=AttackResult.failed;stay.tier=1;stay.weight=1;
            }
        }
        if(stay==null)return original;
        own.getParticipant().switchingIn.remove(original.switchPokemon);
        return stay;
    }
}
