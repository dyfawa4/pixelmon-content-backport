package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;

/** Choose before native send-out; stored party order and player submissions are untouched. */
public final class NpcLead {
    private NpcLead(){}
    public static void choose(TrainerParticipant trainer){
        if(trainer.bc==null||trainer.bc.simulateMode||!NpcGimmicks.enabled(trainer.trainer)||!GymCities.isLeader(trainer.trainer)&&!AuthorBossTeams.is(trainer.trainer))return;
        List<PixelmonWrapper> enemies=new ArrayList<>();
        for(var side:trainer.bc.participants)if(side instanceof PlayerParticipant&&side.team!=trainer.team){
            if(!side.controlledPokemon.isEmpty())enemies.addAll(side.controlledPokemon);
            else if(side.allPokemon.length>0)enemies.add(side.allPokemon[0]);
        }
        if(enemies.isEmpty())return;
        List<PixelmonWrapper> options=new ArrayList<>();List<Double> scores=new ArrayList<>();
        for(var candidate:trainer.allPokemon)if(candidate.isAlive()){
            double value=NpcTeamKnowledge.lead(candidate);
            for(var enemy:enemies){
                double incoming=0,outgoing=0;
                for(var attack:enemy.getMoveset())if(attack.getAttackCategory()!=AttackCategory.STATUS)incoming=Math.max(incoming,Math.max(40,attack.getMove().getBasePower())*effect(enemy,candidate,attack.getType()));
                for(var attack:candidate.getMoveset())if(attack.getAttackCategory()!=AttackCategory.STATUS)outgoing=Math.max(outgoing,Math.max(40,attack.getMove().getBasePower())*effect(candidate,enemy,attack.getType()));
                value+=outgoing-incoming*1.4+candidate.getForm().getBattleStats().getStat(com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.SPEED)*.1;
                if(incoming==0)value+=120;
                boolean setup=enemy.getMoveset().stream().anyMatch(NpcSetupControl::boost);
                if(setup){
                    if(candidate.getMoveset().stream().anyMatch(a->a.getMove().getAttackName().equals("Snatch")))value+=700;
                    if(candidate.getMoveset().stream().anyMatch(a->a.getMove().getAttackName().equals("Snatch"))&&candidate.getMoveset().stream().anyMatch(a->a.getMove().getAttackName().equals("Haze")))value+=700;
                    if(candidate.getMoveset().stream().anyMatch(a->a.getMove().getAttackName().equals("Haze")))value+=50;
                }
            }
            options.add(candidate);scores.add(value);
        }
        double maximum=scores.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        int index=NearBest.choose(scores.stream().mapToDouble(Double::doubleValue).toArray(),Math.min(24,Math.max(8,Math.abs(maximum)*.08)),8,NpcChoiceVariation.random(trainer.allPokemon[0],191));
        PixelmonWrapper best=index<0?null:options.get(index);
        if(best!=null){
            trainer.allPokemon=trainer.allPokemon.clone();
            for(int i=0;i<trainer.allPokemon.length;i++)if(trainer.allPokemon[i]==best){trainer.allPokemon[i]=trainer.allPokemon[0];trainer.allPokemon[0]=best;break;}
            // setupTeam marked a provisional first member before native startBattle.
            // Native setPokemonInBattle only sets true; remove that stale placeholder first.
            for(var member:trainer.allPokemon)member.onBattlefield=false;
        }
    }
    private static double effect(PixelmonWrapper user,PixelmonWrapper target,Element type){
        String ability=target.getBattleAbility()==null?"":target.getBattleAbility().getClass().getSimpleName();
        if(type==Element.GROUND&&ability.equals("Levitate")||type==Element.ELECTRIC&&Set.of("VoltAbsorb","MotorDrive","LightningRod").contains(ability)
            ||type==Element.WATER&&Set.of("StormDrain","WaterAbsorb","DrySkin","DesolateLand").contains(ability)||type==Element.GRASS&&ability.equals("SapSipper")
            ||type==Element.FIRE&&Set.of("FlashFire","PrimordialSea").contains(ability))return 0;
        return Element.getTotalEffectiveness(target.getForm().getTypes(),type)*(user.getForm().getTypes().contains(type)?1.5:1);
    }
}
