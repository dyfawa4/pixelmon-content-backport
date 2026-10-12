package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;

/** Use the native AI's observed moves and type-based guesses, never a hidden enemy moveset. */
public final class NpcTactics {
    private NpcTactics(){}
    private static final Set<String> SETUP=new HashSet<>(Arrays.asList("Swords Dance","Dragon Dance","Bulk Up","Nasty Plot","Calm Mind","Quiver Dance","Shell Smash","Agility","Rock Polish"));
    /** Refine native legal/simulated choices before AdvancedAI compares switches. */
    public static void weight(PixelmonWrapper own,List<MoveChoice> choices){
        if(own.bc==null || own.bc.simulateMode || own.bc.isRaid() || !own.onBattlefield || !own.isAlive()
            || !(own.getParticipant() instanceof com.pixelmonmod.pixelmon.battles.controller.participants.TrainerParticipant)
            || !NpcGimmicks.enabled(((com.pixelmonmod.pixelmon.battles.controller.participants.TrainerParticipant)own.getParticipant()).trainer))return;
        List<MoveChoice> threats=threats(own);double incoming=incoming(own,threats);
        boolean reserve=Arrays.stream(own.getParticipant().allPokemon).anyMatch(p->p!=own && p.isAlive() && !p.onBattlefield);
        for(MoveChoice choice:choices){
            if(!choice.isAttack() || choice.tier<=0 || choice.result!=null && choice.result.result==com.pixelmonmod.pixelmon.battles.controller.log.AttackResult.failed)continue;
            String name=choice.attack.getMove().getAttackName();
            // Native TacticalAI leaves a doomed KO at tier 3 with zero weight.
            // Such a move otherwise outranks every viable tier-2 switch.
            if(choice.isOffensiveMove() && choice.tier>=3 && incoming>=own.getHealth() && !finishFirst(own,choice,threats)){
                choice.lowerTier(1);choice.weight=Math.min(choice.weight,5);
            }
            if(SETUP.contains(name)){
                boolean movesFirst=!MoveChoice.canOutspeed(threats,own,choice.createList());
                boolean payoff=choices.stream().anyMatch(c->c.isOffensiveMove() && c.tier>=2 && c.weight>0);
                // A boost needs a following attacking turn. Avoid boosting into
                // likely defeat, even when the native status simulation succeeds.
                if(!payoff || incoming*(movesFirst?1.35:2.1)>=own.getHealth()){
                    choice.lowerTier(1);choice.weight*=.15f;
                }else if(incoming<own.getHealth()*.3 && own.getBattleStats().getSumIncreases()<4){
                    choice.weight*=1.25f;
                }
            }else if(NpcMoves.recovery(choice.attack)){
                boolean movesFirst=!MoveChoice.canOutspeed(threats,own,choice.createList());
                double after=Math.min(own.getMaxHealth(),own.getHealth()+own.getMaxHealth()*.5);
                if(own.getHealthPercent()>=85 || (!movesFirst && incoming>=own.getHealth()) || incoming>=after){
                    choice.lowerTier(1);choice.weight*=.1f;
                }
            }else if((name.equals("U-turn") || name.equals("Volt Switch") || name.equals("Flip Turn") || name.equals("Parting Shot")) && !reserve && choice.tier<3){
                choice.weight*=.75f;
            }
        }
    }
    public static MoveChoice refine(PixelmonWrapper own,MoveChoice original){
        if(original==null || !original.isAttack() || own.bc.simulateMode || !own.isAlive() || !own.onBattlefield
            || own.choiceLocked!=null || own.getHealthPercent()>35
            || own.getStatus(com.pixelmonmod.pixelmon.battles.status.StatusType.Sleep)!=null
            || own.getStatus(com.pixelmonmod.pixelmon.battles.status.StatusType.Freeze)!=null)return original;
        List<MoveChoice> threats=threats(own);if(finishFirst(own,original,threats))return original;
        for(var status:own.getStatuses())if(status.type.toString().equalsIgnoreCase("Taunt") || status.type.toString().equalsIgnoreCase("HealBlock"))return original;
        double incoming=incoming(own,threats);
        for(Attack attack:own.getMoveset())if(NpcMoves.recovery(attack) && attack.pp>0 && !attack.getDisabled()){
            MoveChoice recovery=new MoveChoice(own,attack,Collections.singletonList(own));
            boolean hitFirst=MoveChoice.canOutspeed(threats,own,recovery.createList());
            // Recover only when it can safely buy a turn; preserve profitable switches and finishing attacks.
            if((!hitFirst || own.getHealth()>incoming) && Math.min(own.getMaxHealth(),own.getHealth()+own.getMaxHealth()*.5)>incoming*1.1)return recovery;
        }
        return original;
    }
    public static List<Attack> knownMoves(PixelmonWrapper own,PixelmonWrapper enemy){
        BattleAIBase ai=own.getBattleAI();
        return ai instanceof TacticalAI?ai.getMoveset(enemy):new OpponentMemory(enemy).getAttacks();
    }
    public static List<MoveChoice> threats(PixelmonWrapper own){
        BattleAIBase ai=own.getBattleAI();if(!(ai instanceof TacticalAI))return Collections.emptyList();
        boolean simulation=own.bc.simulateMode;own.bc.simulateMode=true;
        try{
            List<MoveChoice> moves=new ArrayList<>();
            for(PixelmonWrapper enemy:own.getOpponentPokemon())moves.addAll(ai.getBestAttackChoices(enemy));
            return moves;
        }finally{own.bc.simulateMode=simulation;}
    }
    public static double incoming(PixelmonWrapper own,List<MoveChoice> threats){
        Map<UUID,Double> damage=new HashMap<>();
        for(MoveChoice choice:threats)if(choice.isOffensiveMove() && choice.result!=null && choice.targets.contains(own))
            damage.merge(choice.user.getPokemonUUID(),(double)Math.max(choice.result.damage,choice.result.fullDamage),Math::max);
        return damage.values().stream().mapToDouble(Double::doubleValue).sum();
    }
    public static boolean finishFirst(PixelmonWrapper own,MoveChoice choice,List<MoveChoice> threats){
        if(choice==null || !choice.isOffensiveMove() || choice.tier<3 || choice.result==null || choice.targets.size()!=1)return false;
        PixelmonWrapper target=choice.targets.get(0);
        if(own.isAlly(target) || !target.isAlive())return false;
        // Native canOutspeed asks whether an opponent can move before this user's choice.
        return !MoveChoice.canOutspeed(threats,own,choice.createList());
    }
}
