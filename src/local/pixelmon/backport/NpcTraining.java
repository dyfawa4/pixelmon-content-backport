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
    public static void maximize(Pokemon pokemon){prepare(pokemon);for(var stat:STATS){pokemon.getIVs().setHyperTrained(stat,false);pokemon.getIVs().setStat(stat,31);}pokemon.getStats().recalculateStats();}
    /** Final moves and the intended Mega form determine training, not random template nature. */
    private static final class Training {
        final GymTrainingPlan plan;final boolean physical;
        Training(GymTrainingPlan plan,boolean physical){this.plan=plan;this.physical=physical;}
    }
    private static Training plan(Pokemon pokemon){
        Stats form=pokemon.getForm(),trainedForm=form;
        if(pokemon.getHeldItem().func_77973_b() instanceof MegaStoneItem){
            Stats mega=pokemon.getSpecies().getForm(((MegaStoneItem)pokemon.getHeldItem().func_77973_b()).getMegaForm());
            if(mega!=null)trainedForm=mega;
        }
        int[] base=new int[6];for(int i=0;i<6;i++)base[i]=trainedForm.getBattleStats().getStat(STATS[i]);
        double physical=0,special=0;int utility=0;boolean recovery=false,room=false,boosting=false;
        Set<String> moves=new HashSet<>();
        for(var attack:pokemon.getMoveset()){
            String name=attack.getMove().getAttackName();
            moves.add(name);
            if(attack.getAttackCategory()==AttackCategory.STATUS){
                utility++;recovery|=NpcMoves.recovery(attack) || name.equals("Rest");room|=name.equals("Trick Room");
                boosting|=Arrays.asList("Swords Dance","Dragon Dance","Nasty Plot","Quiver Dance","Shell Smash","Agility","Rock Polish").contains(name);
            }else if(Set.of("Seismic Toss","Night Shade","Super Fang","Nature's Madness","Ruination","Endeavor","Counter","Mirror Coat","Metal Burst","Foul Play","Dragon Rage","Sonic Boom").contains(name)){
                // Fixed damage and Foul Play do not benefit from the user's offensive EVs.
            }else if(attack.getAttackCategory()==AttackCategory.PHYSICAL)physical+=Math.max(0,attack.getMove().getBasePower())*base[name.equals("Body Press")?2:1];
            else special+=Math.max(0,attack.getMove().getBasePower())*base[3];
        }
        return new Training(GymTrainingPlan.choose(base,physical,special,utility,recovery,room,boosting,moves,pokemon.getPokemonLevel()),physical>=special);
    }
    public static void optimizeNature(Pokemon pokemon){applyNature(pokemon,plan(pokemon).plan);}
    private static void applyNature(Pokemon pokemon,GymTrainingPlan plan){
        // Native summary screens expose the base nature; a mint-only change left
        // random template natures visible even when the effective stats differed.
        Nature selected=Nature.valueOf(plan.nature);
        if(pokemon.getMintNature()==null&&pokemon.getBaseNature()==selected)return;
        pokemon.setMintNature(null);pokemon.setNature(selected);
    }
    public static void copyNature(Pokemon source,Pokemon snapshot){
        snapshot.setMintNature(null);snapshot.setNature(source.getBaseNature());snapshot.setMintNature(source.getMintNature());
    }
    public static void prepare(Pokemon pokemon){
        Training training=plan(pokemon);GymTrainingPlan plan=training.plan;
        pokemon.getEVs().fillFromArray(0,0,0,0,0,0);
        for(int i=0;i<6;i++){
            pokemon.getIVs().setHyperTrained(STATS[i],false);pokemon.getIVs().setStat(STATS[i],plan.ivs[i]);
            pokemon.getEVs().setStat(STATS[i],plan.evs[i]);
        }
        applyNature(pokemon,plan);
        chooseAbility(pokemon,training.physical);
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
        copyNature(source,snapshot);snapshot.setAbility(source.getAbility());snapshot.getStats().recalculateStats();
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
