package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.species.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
/** Draft complete legendary combinations, sharing offensive and defensive roles. */
public final class AuthorCorePool {
    public static final class Core {public final int dex;public final String form,item;Core(int d,String f,String i){dex=d;form=f;item=i;}}
    private static final List<Core> POOL=List.of(new Core(383,"primal","pixelmon:red_orb"),new Core(382,"primal","pixelmon:blue_orb"),new Core(493,"","pixelmon:legend_plate"),new Core(888,"crowned","pixelmon:rusted_sword"),new Core(898,"shadowrider",null),new Core(898,"icerider",null),new Core(716,"",null),new Core(717,"",null),new Core(250,"",null),new Core(249,"",null),new Core(487,"altered",null),new Core(483,"",null),new Core(484,"",null),new Core(384,"",null),new Core(1007,"",null),new Core(1008,"",null),new Core(800,"dusk",null));
    private static final Set<String> RECOVERY=Set.of("Recover","Roost","Morning Sun","Moonlight","Oblivion Wing","Rest");
    private AuthorCorePool(){}
    public static Core choose(List<Pokemon> existing,Random random){return draft(existing,1,random).get(0);}
    private static final class Option {
        final Core core;final double[] defence;final int total;final boolean physical,special,recovery,disruption;
        Option(Core core,Stats form,int total){
            this.core=core;this.total=total;defence=AuthorTeamBalance.defences(form);
            physical=form.getBattleStats().getStat(BattleStatsType.ATTACK)>form.getBattleStats().getStat(BattleStatsType.SPECIAL_ATTACK)||core.dex==384||core.dex==717||core.dex==487;
            special=!physical||core.dex==717||core.dex==487;
            recovery=Set.of(249,250,487,493,717,800).contains(core.dex)&&form.getMoves().getAllMoves().stream().anyMatch(a->RECOVERY.contains(a.getAttackName()));
            disruption=AuthorTeamBalance.disruption(form);
        }
    }
    public static List<Core> draft(List<Pokemon> existing,int count,Random random){
        if(count<1||count>3)throw new IllegalArgumentException("Author core count "+count);
        List<Option> options=new ArrayList<>();
        for(Core core:POOL){
            Species species=PixelmonSpecies.fromNationalDex(core.dex);
            if(species==null||existing.stream().anyMatch(p->p.getSpecies().getDex()==core.dex))continue;
            Stats form=core.form.isEmpty()?species.getDefaultForm():species.getForms().stream().filter(f->f.isForm(core.form)).findFirst().orElse(null);
            if(form==null)continue;int total=0;for(var stat:BattleStatModifiers.STATS)total+=form.getBattleStats().getStat(stat);
            if(total<650||form.getMoves().getAllMoves().stream().filter(a->a.getAttackCategory()!=AttackCategory.STATUS&&a.getBasePower()>0).count()<2)continue;
            options.add(new Option(core,form,total));
        }
        List<double[]> base=new ArrayList<>();Set<Integer> dexes=new HashSet<>();boolean physical=false,special=false,recovery=false,disruption=false;
        for(Pokemon p:existing){
            base.add(AuthorTeamBalance.defences(p));dexes.add(p.getSpecies().getDex());disruption|=AuthorTeamBalance.disruption(p);
            physical|=p.getMoveset().stream().anyMatch(a->a.getAttackCategory()==AttackCategory.PHYSICAL&&a.getMove().getBasePower()>0);
            special|=p.getMoveset().stream().anyMatch(a->a.getAttackCategory()==AttackCategory.SPECIAL&&a.getMove().getBasePower()>0);
            recovery|=p.getMoveset().stream().anyMatch(a->RECOVERY.contains(a.getMove().getAttackName()));
        }
        List<List<Core>> combinations=new ArrayList<>();List<Double> scores=new ArrayList<>();
        collect(options,0,count,new ArrayList<>(),base,dexes,physical,special,recovery,disruption,combinations,scores);
        // Compare complete teams within a quality floor instead of fixing a
        // locally best first member before its partners have been considered.
        int selected=NearBest.choose(scores.stream().mapToDouble(Double::doubleValue).toArray(),45,20,random);
        if(selected<0)throw new IllegalStateException("No powerful balanced author legendary combination");
        return combinations.get(selected);
    }
    private static void collect(List<Option> options,int from,int remaining,List<Option> picked,List<double[]> base,Set<Integer> existing,boolean physical,boolean special,boolean recovery,boolean disruption,List<List<Core>> teams,List<Double> scores){
        if(remaining>0){
            for(int i=from;i<options.size();i++){
                Option option=options.get(i);if(picked.stream().anyMatch(p->p.core.dex==option.core.dex))continue;
                picked.add(option);collect(options,i+1,remaining-1,picked,base,existing,physical,special,recovery,disruption,teams,scores);picked.remove(picked.size()-1);
            }return;
        }
        List<double[]> defences=new ArrayList<>(base);Set<Integer> dexes=new HashSet<>(existing);List<Core> team=new ArrayList<>();double value=0;
        for(Option option:picked){
            dexes.add(option.core.dex);team.add(option.core);defences.add(option.defence);physical|=option.physical;special|=option.special;recovery|=option.recovery;disruption|=option.disruption;
            value+=(option.total-600)*.2;
        }
        if(!disruption||!physical||!special)return;
        value+=AuthorTeamBalance.coverage(defences)+(physical&&special?30:0)+(recovery?20:0);
        boolean oldWeatherConflict=existing.contains(382)&&(existing.contains(383)||existing.contains(1007));
        if(!oldWeatherConflict&&dexes.contains(382)&&(dexes.contains(383)||dexes.contains(1007)))return;
        teams.add(team);scores.add(value);
    }
}
