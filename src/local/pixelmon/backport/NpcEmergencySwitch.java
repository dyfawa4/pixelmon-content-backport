package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;

/** Rescue a doomed action only when a native legal reserve can enter and keep a useful reply. */
public final class NpcEmergencySwitch {
    private NpcEmergencySwitch(){}
    public static MoveChoice refine(PixelmonWrapper own,MoveChoice original){
        if(original==null||!original.isAttack()||own.bc==null||own.bc.simulateMode||!NpcTurnKnowledge.present(own.bc)||!NpcTurnKnowledge.enabled(own))return original;
        if(NpcMoveIntent.read(original.attack).has(NpcMoveIntent.Trait.GUARD))return original;
        var current=NpcTactics.threats(own,original);double danger=NpcTactics.incoming(own,current);
        if(danger*1.1<own.getHealth()||NpcTactics.finishFirst(own,original,current))return original;
        var ai=own.getBattleAI();var part=own.getParticipant();int slot=part.controlledPokemon.indexOf(own);if(slot<0)return original;
        MoveChoice best=null;double score=-Double.MAX_VALUE;
        for(var choice:ai.getSwitchChoices(own)){
            var reserve=part.getPokemonFromParty(choice.switchPokemon);
            if(reserve==null||reserve==own||!reserve.isAlive()||reserve.onBattlefield)continue;
            var copy=NpcSimulationCopy.of(reserve);copy.onBattlefield=true;boolean mode=own.bc.simulateMode;
            part.controlledPokemon.set(slot,copy);own.bc.simulateMode=true;
            try{
                int hazards=0;for(var hazard:own.getEntryHazards())if(!hazard.isImmune(copy))hazards+=Math.max(0,hazard.getDamage(copy));
                if(hazards>=copy.getHealth())continue;copy.setHealth(copy.getHealth()-hazards);
                double entry=NpcTactics.incoming(copy,NpcTactics.threats(copy,(MoveChoice)null));
                if(entry*1.1>=copy.getHealth()||entry>=danger*.65)continue;
                double remaining=copy.getHealth()-entry;
                copy.setHealth(Math.max(1,(int)remaining));
                boolean clock=NpcTurnKnowledge.future(own.bc,true);
                try{
                    var future=NpcTactics.threats(copy,true);double next=NpcTactics.incoming(copy,future);
                    MoveChoice reply=null;for(var attack:ai.getBestAttackChoices(copy))if(attack.result!=null&&attack.result.damage>0&&attack.attack.pp>0&&!attack.attack.getDisabled()&&(reply==null||attack.weight>reply.weight))reply=attack;
                    if(reply==null)continue;
                    // The switch itself spends a turn. A reserve that immediately loses
                    // to coverage on the next turn is not a successful counter.
                    if(next*1.1>=remaining&&!NpcTactics.finishFirst(copy,reply,future))continue;
                    double value=remaining/Math.max(1,copy.getMaxHealth())*100-next/Math.max(1,remaining)*40+Math.min(30,reply.weight*.15);
                    if(value>score){score=value;best=choice;}
                }finally{NpcTurnKnowledge.future(own.bc,clock);}
            }finally{part.controlledPokemon.set(slot,own);own.bc.simulateMode=mode;}
        }
        if(best==null)return original;
        best.raiseTier(Math.max(2,original.tier));best.weight=Math.max(100,original.weight+20);
        if(!part.switchingIn.contains(best.switchPokemon))part.switchingIn.add(best.switchPokemon);
        return best;
    }
}
