package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.ability.Ability;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.items.heldItems.MegaStoneItem;

/** Real trainer-owned stats only. No level, base stat, damage or battle multiplier changes. */
public final class NpcTraining {
    private NpcTraining(){}
    private static final BattleStatsType[] STATS={BattleStatsType.HP,BattleStatsType.ATTACK,BattleStatsType.DEFENSE,BattleStatsType.SPECIAL_ATTACK,BattleStatsType.SPECIAL_DEFENSE,BattleStatsType.SPEED};
    public static void prepare(Pokemon pokemon){
        Stats form=pokemon.getForm(),trainedForm=form;
        if(pokemon.getHeldItem().func_77973_b() instanceof MegaStoneItem){
            Stats mega=pokemon.getSpecies().getForm(((MegaStoneItem)pokemon.getHeldItem().func_77973_b()).getMegaForm());
            if(mega!=null)trainedForm=mega;
        }
        int[] base=new int[6];for(int i=0;i<6;i++)base[i]=trainedForm.getBattleStats().getStat(STATS[i]);
        double physical=0,special=0;int utility=0;boolean recovery=false,room=false,boosting=false;
        for(var attack:pokemon.getMoveset()){
            String name=attack.getMove().getAttackName();
            if(attack.getAttackCategory()==AttackCategory.STATUS){
                utility++;recovery|=NpcMoves.recovery(attack) || name.equals("Rest");room|=name.equals("Trick Room");
                boosting|=Arrays.asList("Swords Dance","Dragon Dance","Nasty Plot","Quiver Dance","Shell Smash","Agility","Rock Polish").contains(name);
            }else if(attack.getAttackCategory()==AttackCategory.PHYSICAL)physical+=Math.max(0,attack.getMove().getBasePower())*base[1];
            else special+=Math.max(0,attack.getMove().getBasePower())*base[3];
        }
        GymTrainingPlan plan=GymTrainingPlan.choose(base,physical,special,utility,recovery,room,boosting);
        pokemon.getEVs().fillFromArray(0,0,0,0,0,0);
        for(int i=0;i<6;i++){
            pokemon.getIVs().setHyperTrained(STATS[i],false);pokemon.getIVs().setStat(STATS[i],plan.ivs[i]);
            pokemon.getEVs().setStat(STATS[i],plan.evs[i]);
        }
        pokemon.setMintNature(Nature.valueOf(plan.nature));
        chooseAbility(pokemon,physical>=special);
        pokemon.getStats().recalculateStats();
        pokemon.getPersistentData().func_74768_a("BackportLeaderTraining",1);
        pokemon.getPersistentData().func_74778_a("BackportLeaderRole",plan.role);
    }
    private static void chooseAbility(Pokemon pokemon,boolean physical){
        Ability best=null;double score=-Double.MAX_VALUE;
        for(Ability ability:pokemon.getForm().getAbilities().getAll())if(ability!=null){
            double value=abilityScore(pokemon,ability.getClass().getSimpleName(),physical);
            // Preserve a legal authored choice in an exact tie.
            if(pokemon.getAbility()!=null && pokemon.getAbility().getClass()==ability.getClass())value+=.01;
            if(value>score){score=value;best=ability;}
        }
        if(best!=null && (pokemon.getAbility()==null || pokemon.getAbility().getClass()!=best.getClass()))pokemon.setAbility(best);
    }
    public static void copy(Pokemon source,Pokemon snapshot){
        for(BattleStatsType stat:STATS){snapshot.getIVs().setStat(stat,source.getIVs().getStat(stat));snapshot.getIVs().setHyperTrained(stat,source.getIVs().isHyperTrained(stat));}
        snapshot.getEVs().fillFromArray(source.getEVs().getArray());
        snapshot.setMintNature(source.getMintNature());snapshot.setAbility(source.getAbility());snapshot.getStats().recalculateStats();
        snapshot.getPersistentData().func_74768_a("BackportLeaderTraining",1);
        snapshot.getPersistentData().func_74778_a("BackportLeaderRole",source.getPersistentData().func_74779_i("BackportLeaderRole"));
    }
    private static double abilityScore(Pokemon pokemon,String name,boolean physical){
        switch(name){
            case "Truant":case "SlowStart":case "Defeatist":return -100;
            case "HugePower":case "PurePower":return physical?150:50;
            case "DesolateLand":case "PrimordialSea":case "DeltaStream":return 140;
            case "Protean":case "Libero":case "MagicBounce":case "SpeedBoost":return 115;
            case "Drizzle":case "Drought":return 110;
            case "Regenerator":case "Multiscale":case "MagicGuard":case "Unaware":case "Adaptability":return 105;
            case "Intimidate":case "Scrappy":case "SheerForce":case "SandStream":case "SnowWarning":return 95;
            case "Moxie":return physical?100:50;
            case "Levitate":return Element.getTotalEffectiveness(pokemon.getForm().getTypes(),Element.GROUND)>1?110:90;
            case "Contrary":return pokemon.getMoveset().stream().anyMatch(a->Arrays.asList("Leaf Storm","Superpower","Overheat","Draco Meteor").contains(a.getMove().getAttackName()))?125:65;
            case "Technician":return pokemon.getMoveset().stream().anyMatch(a->a.getAttackCategory()!=AttackCategory.STATUS && a.getMove().getBasePower()>0 && a.getMove().getBasePower()<=60)?105:65;
            case "Guts":case "PoisonHeal":
                String item=pokemon.getHeldItem().func_77973_b().getRegistryName().func_110623_a();
                return item.equals("flame_orb") || item.equals("toxic_orb")?125:65;
            default:return 75;
        }
    }
}
