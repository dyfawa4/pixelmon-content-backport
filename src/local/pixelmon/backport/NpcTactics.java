package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;

/** Native damage previews, with submitted-action knowledge scoped to eligible NPC planning. */
public final class NpcTactics {
    private NpcTactics(){}
    /** Refine native legal/simulated choices before AdvancedAI compares switches. */
    public static void weight(PixelmonWrapper own,List<MoveChoice> choices){
        if(own.bc==null || own.bc.simulateMode || own.bc.isRaid() || !own.onBattlefield || !own.isAlive()
            || !(own.getParticipant() instanceof com.pixelmonmod.pixelmon.battles.controller.participants.TrainerParticipant)
            || !NpcGimmicks.enabled(((com.pixelmonmod.pixelmon.battles.controller.participants.TrainerParticipant)own.getParticipant()).trainer))return;
        NpcIntentResponse.weight(own,choices);
        List<MoveChoice> threats=threats(own);double attackRisk=0;
        if(NpcTurnKnowledge.active(own.bc))for(var candidate:choices)if(candidate.isOffensiveMove())
            attackRisk=Math.max(attackRisk,incoming(own,threats(own,candidate)));
        boolean reserve=Arrays.stream(own.getParticipant().allPokemon).anyMatch(p->p!=own && p.isAlive() && !p.onBattlefield);
        for(MoveChoice choice:choices){
            if(!choice.isAttack() || choice.tier<=0 || choice.result!=null && choice.result.result==com.pixelmonmod.pixelmon.battles.controller.log.AttackResult.failed)continue;
            List<MoveChoice> response=NpcTurnKnowledge.active(own.bc)?threats(own,choice):threats;
            double incoming=incoming(own,response);
            String name=choice.attack.getMove().getAttackName();
            if(LightningOath.signature(own,choice.attack)&&choice.targets.size()==1&&LightningOath.remaining(choice.targets.get(0))<2&&incoming<own.getHealth()*.65&&choice.result!=null&&choice.result.damage<choice.targets.get(0).getHealth())choice.weight+=25;
            if(NpcTurnKnowledge.active(own.bc)&&choice.isOffensiveMove()&&choice.targets.size()==1&&choice.result!=null){
                var target=choice.targets.get(0);double hp=NpcTurnKnowledge.health(target);
                // Items resolve before moves. A KO scored against pre-potion HP is no longer a KO.
                if(choice.tier>=3&&(NpcTurnKnowledge.arrival(target)!=target||choice.result.damage<hp))choice.lowerTier(2);
            }
            // Native TacticalAI leaves a doomed KO at tier 3 with zero weight.
            // Such a move otherwise outranks every viable tier-2 switch.
            if(choice.isOffensiveMove() && choice.tier>=3 && incoming>=own.getHealth() && !finishFirst(own,choice,response)){
                choice.lowerTier(1);choice.weight=Math.min(choice.weight,5);
            }
            if(choice.attack.getAttackCategory()==com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS&&choice.tier>=2&&choice.weight>0&&attackRisk>incoming){
                choice.weight+=(float)Math.min(90,(attackRisk-incoming)/Math.max(1,own.getMaxHealth())*100);
            }
            if(choice.attack.getAttackCategory()==com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS&&NpcMoveIntent.read(choice.attack).has(NpcMoveIntent.Trait.BOOSTS_SELF)){
                boolean movesFirst=!MoveChoice.canOutspeed(response,own,choice.createList());
                boolean payoff=own.getMoveset().stream().anyMatch(a->a.pp>0&&!a.getDisabled()&&a.getAttackCategory()!=com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS);
                // A boost needs a following attacking turn. Avoid boosting into
                // likely defeat, even when the native status simulation succeeds.
                if(!payoff || incoming*(movesFirst?1.35:2.1)>=own.getHealth()){
                    choice.lowerTier(1);choice.weight*=.15f;
                }else if(incoming<own.getHealth()*.3 && own.getBattleStats().getSumIncreases()<4){
                    choice.weight*=1.25f;
                }
            }else if(NpcMoves.recovery(choice.attack)){
                boolean movesFirst=!MoveChoice.canOutspeed(response,own,choice.createList());
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
        if(NpcTurnKnowledge.enabled(own)){List<Attack> submitted=NpcTurnKnowledge.moves(enemy);if(submitted!=null)return submitted;}
        BattleAIBase ai=own.getBattleAI();
        return ai instanceof TacticalAI?ai.getMoveset(enemy):new OpponentMemory(enemy).getAttacks();
    }
    public static List<MoveChoice> threats(PixelmonWrapper own){
        return threats(own,false);
    }
    public static List<MoveChoice> threats(PixelmonWrapper own,MoveChoice action){
        try(var preview=NpcTurnKnowledge.preview(own,action!=null&&action.isAttack()?action.attack:null)){return threats(own,false);}
    }
    public static double defensiveRisk(PixelmonWrapper own,com.pixelmonmod.pixelmon.api.pokemon.Element type){
        var original=own.type;
        try{if(type!=com.pixelmonmod.pixelmon.api.pokemon.Element.MYSTERY)own.type=Collections.singletonList(type);return incoming(own,threats(own));}
        finally{own.type=original;}
    }
    public static List<MoveChoice> threats(PixelmonWrapper own,boolean future){
        BattleAIBase ai=own.getBattleAI();if(!(ai instanceof TacticalAI))return Collections.emptyList();
        boolean previous=NpcTurnKnowledge.future(own.bc,future);
        boolean simulation=own.bc.simulateMode;own.bc.simulateMode=true;
        try{
            List<MoveChoice> moves=new ArrayList<>();
            for(PixelmonWrapper enemy:own.getOpponentPokemon()){
                List<Attack> submitted=NpcTurnKnowledge.moves(enemy);
                if(submitted!=null){
                    // Evaluate the committed move directly, not a "best choices"
                    // shortlist that may discard it or refer to the old target UUID.
                    if(future||NpcTurnKnowledge.targets(enemy,own))for(Attack attack:submitted)if(attack.getAttackCategory()!=com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS){
                        MoveChoice hit=new MoveChoice(enemy,attack,Collections.singletonList(own));
                        Attack saved=enemy.attack,selected=enemy.selectedAttack;var targets=enemy.targets;boolean evolve=enemy.willEvolve,z=enemy.usingZ;
                        try{((local.pixelmon.backport.mixin.NpcDamagePreviewAccess)ai).backport$damage(enemy,hit);if(!future)NpcIntentResponse.conditional(ai,hit);moves.add(hit);}
                        finally{enemy.attack=saved;enemy.selectedAttack=selected;enemy.targets=targets;enemy.willEvolve=evolve;enemy.usingZ=z;}
                    }
                }else moves.addAll(ai.getBestAttackChoices(future?NpcTurnKnowledge.arrival(enemy):enemy));
            }
            return NpcTurnKnowledge.restrict(own.bc,moves);
        }finally{own.bc.simulateMode=simulation;NpcTurnKnowledge.future(own.bc,previous);}
    }
    public static double incoming(PixelmonWrapper own,List<MoveChoice> threats){
        Map<UUID,Double> damage=new HashMap<>();
        for(MoveChoice choice:threats)if(choice.isOffensiveMove() && choice.result!=null && choice.result.result!=com.pixelmonmod.pixelmon.battles.controller.log.AttackResult.failed && choice.targets.contains(own))
            damage.merge(choice.user.getPokemonUUID(),(double)Math.max(choice.result.damage,choice.result.fullDamage),Math::max);
        return damage.values().stream().mapToDouble(Double::doubleValue).sum();
    }
    /** Spending a finite item needs an upper estimate, not the native average roll. */
    public static double medicineDamage(PixelmonWrapper own,List<MoveChoice> threats){
        Map<UUID,Double> damage=new HashMap<>();
        for(MoveChoice choice:threats)if(choice.isOffensiveMove()&&choice.targets.contains(own)){
            if(choice.result==null)return Double.POSITIVE_INFINITY;
            if(choice.result.result==com.pixelmonmod.pixelmon.battles.controller.log.AttackResult.failed)continue;
            double raw=Math.max(choice.result.damage,choice.result.fullDamage);
            // Native simulation represents random 2-5 hits as 3.168 hits.
            // For item safety use the possible five hits instead.
            for(var effect:choice.attack.getMove().effects)if(effect instanceof com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.attackModifiers.MultipleHit){
                var hits=(com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.attackModifiers.MultipleHit)effect;
                boolean fixed=choice.user.getBattleAbility() instanceof com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.SkillLink
                    ||AshWaterBoost.eligible(choice.user)&&!choice.user.usingZ&&choice.attack.getMove().getAttackName().equals("Water Shuriken");
                if(hits.minHits==2&&hits.maxHits==5&&!fixed)raw*=5/3.168;
            }
            damage.merge(choice.user.getPokemonUUID(),Math.ceil(raw/.85),Math::max);
        }
        double residual=0;
        for(var status:own.getStatuses()){
            String name=status.type.toString();
            // A Max Potion does not cure these. Avoid spending it on escalating
            // toxic damage; ordinary residuals keep a conservative safety reserve.
            if(name.equals("PoisonBad"))return Double.POSITIVE_INFINITY;
            if(Arrays.asList("Poison","Burn","LeechSeed","Curse").contains(name))residual+=own.getMaxHealth()*(name.equals("Curse")?.25:.125);
        }
        return damage.values().stream().mapToDouble(Double::doubleValue).sum()+residual;
    }
    public static boolean finishFirst(PixelmonWrapper own,MoveChoice choice,List<MoveChoice> threats){
        return knocksOut(own,choice)&&!MoveChoice.canOutspeed(threats,own,choice.createList());
    }
    private static boolean knocksOut(PixelmonWrapper own,MoveChoice choice){
        if(choice==null || !choice.isOffensiveMove() || choice.tier<3 || choice.result==null || choice.targets.size()!=1)return false;
        PixelmonWrapper target=choice.targets.get(0);
        if(own.isAlly(target) || !target.isAlive() || choice.result.result==com.pixelmonmod.pixelmon.battles.controller.log.AttackResult.failed)return false;
        double health=NpcTurnKnowledge.active(own.bc)?NpcTurnKnowledge.health(target):target.getHealth();
        // Damage is capped at remaining HP. Applying a safety margin to that cap
        // made every submitted-action KO fail this check, even a massive overkill.
        if(choice.result.damage<health)return false;
        if(NpcTurnKnowledge.active(own.bc)&&(NpcTurnKnowledge.arrival(target)!=target
            ||Math.max(choice.result.damage,choice.result.fullDamage)*.9<health))return false;
        return true;
    }
    /** Reliable immediate KOs outrank status/secondary-effect scores within the native legal pool. */
    public static MoveChoice finisher(PixelmonWrapper own,List<MoveChoice> choices){
        List<MoveChoice> threats=threats(own);MoveChoice best=null;int accuracy=-1;
        for(var choice:choices)if(choice.user==own&&choice.attack!=null&&choice.attack.pp>0&&!choice.attack.getDisabled()
            &&Float.isFinite(choice.weight)&&choice.weight>0&&knocksOut(own,choice)){
            List<MoveChoice> response=NpcTurnKnowledge.active(own.bc)?threats(own,choice):threats;
            // A slower KO is useful only if the known reply cannot stop it.
            // Emergency last-survivor medicine still requires a first-strike KO.
            if(MoveChoice.canOutspeed(response,own,choice.createList())&&incoming(own,response)*1.1>=own.getHealth())continue;
            int hit=choice.result.accuracy<0?100:choice.result.accuracy;
            if(hit>0&&(best==null||hit>accuracy||hit==accuracy&&choice.weight>best.weight)){best=choice;accuracy=hit;}
        }
        return best;
    }
}
