package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.log.AttackResult;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.StatusType;
import com.pixelmonmod.pixelmon.items.heldItems.FocusSashItem;

/** Small, explainable suggestions within the existing native legal candidate pool. */
public final class NpcMatchupAdvice {
    private NpcMatchupAdvice(){}
    private static final Map<String,String> WEATHER=Map.of("Rain Dance","Rain","Sunny Day","Sunny","Sandstorm","Sandstorm","Hail","Hail");
    public static float score(PixelmonWrapper own,MoveChoice choice){
        if(choice.user!=own||choice.attack==null||choice.attack.pp<=0||choice.attack.getDisabled()||choice.tier<2
            ||choice.result==null||choice.result.result==AttackResult.failed||!Float.isFinite(choice.weight)||choice.weight<=0)return 0;
        String name=choice.attack.getMove().getAttackName();float score=0;
        for(var foe:own.getOpponentPokemon())if(foe.isAlive()){
            // Multi-hit is useful before the sash activates, not after it has been consumed.
            if(choice.isOffensiveMove()&&choice.targets.contains(foe)&&choice.result.damage>0
                &&NpcTurnKnowledge.itemView(foe).getUsableHeldItem() instanceof FocusSashItem
                &&NpcTurnKnowledge.health(foe)>=foe.getMaxHealth()&&NpcSurvival.minimumHits(choice)>1)score+=12;
            if(choice.isOffensiveMove()&&choice.targets.contains(foe)&&choice.result.damage>0&&name.equals("Knock Off")
                &&foe.hasHeldItem()&&foe.isItemRemovable(own))score+=8;
            // The native simulator temporarily writes attack. Read the scoped
            // committed action, or the last observed move, never that scratch field.
            var submitted=NpcTurnKnowledge.moves(foe);
            var reply=submitted!=null&&submitted.size()==1?submitted.get(0):foe.lastAttack;
            boolean setupOrHeal=reply!=null&&(NpcMoveIntent.read(reply).has(NpcMoveIntent.Trait.BOOSTS_SELF)||NpcMoves.recovery(reply));
            if(choice.targets.contains(foe)&&setupOrHeal){
                var intent=NpcMoveIntent.read(choice.attack);
                if(intent.has(NpcMoveIntent.Trait.BLOCKS_STATUS))score+=12;
                if(intent.has(NpcMoveIntent.Trait.FORCES_LAST_MOVE)&&foe.choiceLocked==null&&foe.lastAttack!=null
                    &&foe.lastAttack.getAttackCategory()==com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS)score+=10;
            }
            if(Set.of("Roar","Whirlwind","Dragon Tail","Circle Throw").contains(name)&&NpcMomentum.pressure(foe)>0
                &&Arrays.stream(foe.getParticipant().allPokemon).anyMatch(p->p.isAlive()&&!p.onBattlefield))score+=14;
        }
        if(WEATHER.containsKey(name)){
            String wanted=WEATHER.get(name);var weather=own.bc.globalStatusController.getWeather();
            if(weather==null||!weather.type.toString().equalsIgnoreCase(wanted)){
                double allies=Arrays.stream(own.getParticipant().allPokemon).filter(PixelmonWrapper::isAlive)
                    .mapToDouble(p->weatherAffinity(p,wanted)).average().orElse(0);
                double foes=(NpcTeamKnowledge.has(own.bc)?NpcTeamKnowledge.opponents(own):own.getOpponentPokemon()).stream().mapToDouble(p->weatherAffinity(p,wanted)).average().orElse(0);
                score+=(float)Math.max(-12,Math.min(12,(allies-foes)*6));
            }
        }
        if(name.equals("Trick Room")&&own.getOpponentPokemon().size()==1){
            var foe=own.getOpponentPokemon().get(0);double speed=own.getBattleStats().getStatWithMod(com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.SPEED);
            double enemy=foe.getBattleStats().getStatWithMod(com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.SPEED);
            if(NpcTeamKnowledge.has(own.bc)){double ours=Arrays.stream(own.getParticipant().allPokemon).filter(PixelmonWrapper::isAlive).mapToDouble(p->p.getBattleStats().getStatWithMod(com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.SPEED)).average().orElse(speed);double theirs=NpcTeamKnowledge.opponents(own).stream().mapToDouble(p->p.getBattleStats().getStatWithMod(com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.SPEED)).average().orElse(enemy);speed=speed*.5+ours*.5;enemy=enemy*.5+theirs*.5;}
            boolean room=own.bc.globalStatusController.hasStatus(StatusType.TrickRoom);
            // Native weighting decides actual turn order; this only breaks close strategic ties.
            score+=(float)Math.max(-12,Math.min(12,(enemy-speed)/6))*(room?-1:1);
        }
        return Math.max(-20,Math.min(20,score));
    }
    private static double weatherAffinity(PixelmonWrapper mon,String weather){
        String ability=mon.getBattleAbility().getClass().getSimpleName();double score=0;
        if(weather.equals("Rain")){
            if(Set.of("SwiftSwim","RainDish","Hydration","DrySkin").contains(ability))score+=2;
            if(mon.getMoveset().stream().anyMatch(a->a.pp>0&&a.getType()==Element.WATER))score++;
            if(mon.getMoveset().stream().anyMatch(a->a.pp>0&&a.getType()==Element.FIRE))score--;
        }else if(weather.equals("Sunny")){
            if(Set.of("Chlorophyll","SolarPower","FlowerGift","OrichalcumPulse","Protosynthesis").contains(ability))score+=2;
            if(mon.getMoveset().stream().anyMatch(a->a.pp>0&&a.getType()==Element.FIRE))score++;
            if(mon.getMoveset().stream().anyMatch(a->a.pp>0&&a.getType()==Element.WATER))score--;
        }else if(weather.equals("Sandstorm")){
            if(Set.of("SandRush","SandForce","SandVeil").contains(ability))score+=2;
            if(mon.hasType(Element.ROCK))score++;
        }else if(weather.equals("Hail")){
            if(Set.of("SlushRush","IceBody","SnowCloak").contains(ability))score+=2;
            if(mon.hasType(Element.ICE))score++;
        }
        return score;
    }
    public static void weight(PixelmonWrapper own,List<MoveChoice> choices){for(var choice:choices)choice.weight+=score(own,choice);}
}
