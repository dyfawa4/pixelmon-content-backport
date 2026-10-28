package local.pixelmon.backport;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.log.AttackResult;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
/** Vary equivalent live plans after tactical evaluation. Native hypothetical previews stay deterministic. */
public final class NpcChoiceVariation {
    private static final Map<BattleController,Long> SEEDS=new WeakHashMap<>();
    private NpcChoiceVariation(){}
    public static Random random(PixelmonWrapper own,long purpose){
        long seed=SEEDS.computeIfAbsent(own.bc,b->ThreadLocalRandom.current().nextLong());
        UUID id=own.getPokemonUUID();
        return new Random(seed^id.getMostSignificantBits()^id.getLeastSignificantBits()^((long)own.bc.battleTurn*0x9e3779b97f4a7c15L)^purpose);
    }
    private static boolean valid(MoveChoice choice,PixelmonWrapper own){
        if(choice==null||choice.user!=own||choice.tier<=0||!Float.isFinite(choice.weight)||choice.weight<=0)return false;
        if(choice.isAttack())return choice.attack.pp>0&&!choice.attack.getDisabled()
            &&(own.choiceLocked==null||own.choiceLocked.getMove().equals(choice.attack.getMove()))
            &&(choice.result==null||choice.result.result!=AttackResult.failed);
        return choice.switchPokemon!=null;
    }
    public static MoveChoice choose(PixelmonWrapper own,List<MoveChoice> pool){
        MoveChoice best=null;
        for(var choice:pool)if(valid(choice,own)&&(best==null||choice.tier>best.tier||choice.tier==best.tier&&choice.weight>best.weight))best=choice;
        if(best==null)return null;
        // Tier 3 also carries forced tactical responses (Haze, Snatch, Taunt).
        // Keep their original precedence, including ties. Finishing blows are selected before this method.
        if(own.bc.simulateMode||best.tier!=2||!NpcTurnKnowledge.enabled(own))return best;
        List<MoveChoice> options=new ArrayList<>();Set<String> seen=new HashSet<>();
        double gap=Math.min(10,Math.max(1,best.weight*.08));
        for(var choice:pool)if(valid(choice,own)&&choice.tier==best.tier&&choice.weight>=best.weight-gap){
            String key=choice.switchPokemon!=null?"switch:"+choice.switchPokemon:choice.attack.getMove().getAttackName()+":"+choice.targets.stream().map(p->p.getPokemonUUID().toString()).sorted().collect(java.util.stream.Collectors.joining(","));
            if(seen.add(key))options.add(choice);
        }
        if(options.size()<2)return best;
        // Stable order and per-turn seed: evaluating the same state again cannot reroll the plan.
        options.sort(Comparator.comparing(c->c.switchPokemon!=null?"switch:"+c.switchPokemon:c.attack.getMove().getAttackName()+c.targets.stream().map(p->p.getPokemonUUID().toString()).sorted().collect(java.util.stream.Collectors.joining(","))));
        double[] scores=options.stream().mapToDouble(c->c.weight).toArray();
        return options.get(NearBest.choose(scores,gap,Math.max(1,gap*.5),random(own,190)));
    }
    public static void end(BattleController battle){SEEDS.remove(battle);}
}
