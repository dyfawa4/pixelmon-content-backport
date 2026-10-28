package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.battles.attacks.*;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.*;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.log.AttackResult;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.StatusType;
/** Compare legal setup control with the actual action order and the following attacking turn. */
public final class NpcSetupControl {
    private NpcSetupControl(){}
    public static Attack submitted(PixelmonWrapper foe){var moves=NpcTurnKnowledge.moves(foe);return moves!=null&&moves.size()==1&&NpcActionAvailability.canAct(foe,moves.get(0))?moves.get(0):null;}
    public static boolean boost(Attack move){return move!=null&&move.getAttackCategory()==AttackCategory.STATUS&&AuthorChallenge.selfBuff(move.getMove());}
    public static boolean pass(Attack move){return move!=null&&move.getMove().getAttackName().equals("Baton Pass");}
    public static PixelmonWrapper projected(PixelmonWrapper foe,Attack move){
        var copy=NpcSimulationCopy.of(foe);if(!boost(move))return copy;
        String ability=foe.getBattleAbility().getClass().getSimpleName();
        for(var effect:move.getMove().effects){BattleStatsType type=null;int amount=0;
            if(effect instanceof StatsEffect){var e=(StatsEffect)effect;type=e.type;amount=e.amount;}
            if(effect instanceof StatsEffectPost){var e=(StatsEffectPost)effect;type=e.type;amount=e.amount;}
            if(effect.getClass().getSimpleName().equals("Minimize")){type=BattleStatsType.EVASION;amount=2;}
            if(effect.getClass().getSimpleName().equals("BellyDrum")){type=BattleStatsType.ATTACK;amount=6-copy.getBattleStats().getStage(type);}
            if(type==null)continue;if(ability.equals("Simple"))amount*=2;if(ability.equals("Contrary"))amount=-amount;
            stage(copy,type,Math.max(-6,Math.min(6,copy.getBattleStats().getStage(type)+amount)));
        }
        refresh(copy);return copy;
    }
    private static void stage(PixelmonWrapper copy,BattleStatsType type,int value){
        int index=type.getStatIndex();if(index<0)return;var stats=copy.getBattleStats();int old=stats.getStage(type);stats.getStages()[index]=value;
        double base=(type==BattleStatsType.ACCURACY||type==BattleStatsType.EVASION)?3d:2d;
        double before=old>=0?(base+old)/base:base/(base-old),after=value>=0?(base+value)/base:base/(base-value);double ratio=after/before;
        switch(type){case ATTACK:stats.attackModifier=(int)Math.round(stats.attackModifier*ratio);break;case DEFENSE:stats.defenseModifier=(int)Math.round(stats.defenseModifier*ratio);break;case SPECIAL_ATTACK:stats.specialAttackModifier=(int)Math.round(stats.specialAttackModifier*ratio);break;case SPECIAL_DEFENSE:stats.specialDefenseModifier=(int)Math.round(stats.specialDefenseModifier*ratio);break;case SPEED:stats.speedModifier=(int)Math.round(stats.speedModifier*ratio);break;case ACCURACY:stats.accuracy=(int)Math.round(stats.accuracy*ratio);break;case EVASION:stats.evasion=(int)Math.round(stats.evasion*ratio);break;default:break;}
    }
    private static void refresh(PixelmonWrapper copy){boolean mode=copy.bc.simulateMode;copy.bc.simulateMode=true;try{copy.bc.modifyStats(copy);}finally{copy.bc.simulateMode=mode;}}
    public static boolean reliableHit(PixelmonWrapper own,MoveChoice choice,PixelmonWrapper foe){
        if(AuthorForesight.visible(own))return AuthorForesight.succeeds(own,foe,choice.attack);
        var attack=choice.attack.deepCopy();var access=(local.pixelmon.backport.mixin.AuthorAccuracyAccess)(Object)attack;
        try(var preview=NpcTurnKnowledge.preview(own,attack)){
            attack.moveAccuracy=access.backport$modifiedStats(own,foe,own.getBattleAbility(),foe.getBattleAbility(),attack.getMove().getBasePower())[1];
            return attack.moveAccuracy<0||attack.cantMiss(own)||access.backport$liveAccuracy(own,foe,own.getBattleAbility())>=95;
        }
    }
    private static boolean followup(PixelmonWrapper own,PixelmonWrapper foe,boolean copyBoosts){
        var next=NpcSimulationCopy.of(own);if(copyBoosts)for(var stat:BattleStatsType.values())if(stat!=BattleStatsType.HP)stage(next,stat,foe.getBattleStats().getStage(stat));
        refresh(next);
        for(var move:own.getMoveset())if(move.pp>0&&!move.getDisabled()&&move.getAttackCategory()!=AttackCategory.STATUS){
            var hit=NpcIntentResponse.raw(own.getBattleAI(),next,move,foe);if(hit.result==null||hit.result.damage<=0||hit.result.result==AttackResult.failed)continue;
            boolean fast=true;for(var enemy:foe.getMoveset())if(enemy.pp>0&&!enemy.getDisabled()&&enemy.getAttackCategory()!=AttackCategory.STATUS&&!NpcIntentResponse.first(next,move,foe,enemy)){fast=false;break;}
            if(fast)return true;
        }
        return false;
    }
    public static void weight(PixelmonWrapper own,List<MoveChoice> choices){
        if(!NpcTurnKnowledge.active(own.bc)||own.getOpponentPokemon().size()!=1)return;
        var foe=own.getOpponentPokemon().get(0);var action=submitted(foe);boolean setting=boost(action),passing=pass(action);
        if(NpcTurnKnowledge.moves(foe)!=null){
            var intent=NpcMoveIntent.read(action);
            boolean selfStatus=action!=null&&action.getAttackCategory()==AttackCategory.STATUS&&!foe.usingZ&&!passing
                &&NpcTurnKnowledge.targets(foe,foe)&&!NpcTurnKnowledge.targets(foe,own)
                &&!intent.has(NpcMoveIntent.Trait.GUARD)&&!intent.has(NpcMoveIntent.Trait.PUNISHES_KO)
                &&!intent.has(NpcMoveIntent.Trait.REFLECTS_STATUS)&&!intent.has(NpcMoveIntent.Trait.STEALS_BUFF);
            if(!selfStatus)for(var choice:choices)if(choice.attack!=null&&choice.attack.getMove().getAttackName().equals("Snatch"))NpcIntentResponse.failed(choice);
        }
        int current=NpcMomentum.pressure(foe);if(!setting&&!passing&&current<=0)return;
        for(var choice:choices){
            if(choice.attack==null||choice.attack.pp<=0||choice.attack.getDisabled()||choice.result==null)continue;
            String name=choice.attack.getMove().getAttackName();boolean status=choice.attack.getAttackCategory()==AttackCategory.STATUS;
            if(status&&own.getStatus(StatusType.Taunt)!=null)continue;
            if(own.choiceLocked!=null&&!own.choiceLocked.getMove().equals(choice.attack.getMove()))continue;
            if(status&&own.getUsableHeldItem()!=null&&own.getUsableHeldItem().getClass().getSimpleName().equals("AssaultVestItem"))continue;
            boolean first=action==null||NpcIntentResponse.first(own,choice.attack,foe,action);
            var threats=NpcTactics.threats(own,choice);double incoming=NpcTactics.incoming(own,threats);
            boolean survives=first||incoming*1.1<own.getHealth();
            if(!survives){
                if(status&&Set.of("Haze","Snatch","Taunt","Encore","Psych Up","Heart Swap","Topsy-Turvy","Roar","Whirlwind").contains(name)){
                    choice.lowerTier(1);choice.weight*=.1f;
                }
                continue;
            }
            if(name.equals("Haze")){
                var after=setting&&!first?projected(foe,action):foe;
                int gain=after.getBattleStats().getSumIncreases()-own.getBattleStats().getSumIncreases();
                if(gain<=0)continue;
                if(!first&&incoming*1.1>=own.getHealth())continue;
                // An early reset does not stop a fresh boost that happens afterwards.
                if(setting&&first&&current==0)continue;
                if(current>0&&!setting){var clean=NpcSimulationCopy.of(foe);clean.getBattleStats().clearBattleStats(true);if(action!=null&&action.getAttackCategory()!=AttackCategory.STATUS){var hit=NpcIntentResponse.raw(own.getBattleAI(),clean,action,own);if(first&&hit.result!=null&&Math.max(hit.result.damage,hit.result.fullDamage)*1.1>=own.getHealth())continue;}}
                choice.result.result=AttackResult.hit;choice.raiseTier(3);choice.weight=Math.max(choice.weight,170+Math.min(120,gain*15));
            }else if(choice.result.result!=AttackResult.failed&&name.equals("Snatch")&&setting&&!foe.usingZ&&first){
                // Native Snatch targets self and runs at +4; it also denies a boost with no offensive payoff.
                choice.raiseTier(3);choice.weight=Math.max(choice.weight,220);
            }else if(choice.result.result!=AttackResult.failed&&Set.of("Taunt","Encore").contains(name)){
                if(name.equals("Encore")&&(foe.lastAttack==null||!boost(foe.lastAttack)))continue;
                if(passing&&!first)continue;
                if(setting||passing){choice.raiseTier(3);choice.weight=Math.max(choice.weight,first?200:155);}
            }else if(choice.result.result!=AttackResult.failed&&Set.of("Psych Up","Heart Swap","Topsy-Turvy").contains(name)&&current>0){
                boolean copying=name.equals("Psych Up");
                double risk=incoming;
                if(first&&!copying&&action!=null&&action.getAttackCategory()!=AttackCategory.STATUS){
                    var changedFoe=NpcSimulationCopy.of(foe);var changedOwn=NpcSimulationCopy.of(own);
                    for(var stat:BattleStatsType.values())if(stat.getStatIndex()>=0){int theirs=foe.getBattleStats().getStage(stat),ours=own.getBattleStats().getStage(stat);stage(changedFoe,stat,name.equals("Heart Swap")?ours:-theirs);if(name.equals("Heart Swap"))stage(changedOwn,stat,theirs);}
                    refresh(changedFoe);refresh(changedOwn);var hit=NpcIntentResponse.raw(own.getBattleAI(),changedFoe,action,changedOwn);risk=hit.result==null?Double.POSITIVE_INFINITY:Math.max(hit.result.damage,hit.result.fullDamage);
                }
                if(risk*(first?1.1:2.1)>=own.getHealth()||copying&&!followup(own,foe,true)){choice.lowerTier(1);choice.weight*=.1f;continue;}
                choice.raiseTier(3);choice.weight=Math.max(choice.weight,150);
            }else if(choice.result.result!=AttackResult.failed&&Set.of("Roar","Whirlwind","Dragon Tail","Circle Throw","Clear Smog","Spectral Thief").contains(name)&&current>0){
                if(!choice.targets.contains(foe)||name.equals("Clear Smog")&&foe.getStatus(StatusType.Substitute)!=null)continue;
                if(choice.isOffensiveMove()&&choice.result.damage<=0)continue;
                if(choice.result.accuracy>=0&&choice.result.accuracy<85)continue;
                choice.raiseTier(3);choice.weight=Math.max(choice.weight,165);
            }
        }
        if(foe.getBattleStats().getEvasionStage()>0&&choices.stream().anyMatch(c->c.attack!=null&&c.attack.getMove().getAttackName().equals("Haze")&&c.tier>=3&&c.weight>0))
            for(var choice:choices)if(choice.isOffensiveMove()&&choice.targets.contains(foe)&&choice.tier>=3&&!reliableHit(own,choice,foe)){choice.lowerTier(2);choice.weight*=.5f;}
    }
}
