package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.battles.attacks.*;
/** Team roles use the form's native learnset, with two damaging slots kept per member. */
public final class GymCounterTeam {
    private GymCounterTeam(){}
    private static final Map<Stats,Integer> CACHE=new IdentityHashMap<>();
    private static final Set<String> KEEP=Set.of("Ten Million Volts","Water Shuriken","Haze","Snatch","Spectral Thief");
    public static void clearCache(){CACHE.clear();}
    public static int capabilities(Stats form){return CACHE.computeIfAbsent(form,f->{int mask=0;for(var a:f.getMoves().getAllMoves()){String n=a.getAttackName();if(n.equals("Haze"))mask|=1;if(n.equals("Snatch"))mask|=2;if(n.equals("Sucker Punch")||n.equals("Spectral Thief"))mask|=4;}if((mask&3)==3)mask|=8;if((mask&7)==7&&f.getTypes().contains(Element.DARK))mask|=16;return mask;});}
    public static int capabilities(List<Pokemon> team){int mask=0;for(var p:team)mask|=capabilities(p.getForm());return mask;}
    public static double coverage(int mask){return 500*Integer.bitCount(mask&31)-1500*Integer.bitCount((~mask)&31);}
    private static boolean legal(Pokemon p,String name){return p.getForm().getMoves().getAllMoves().stream().anyMatch(a->a.getAttackName().equals(name));}
    private static int offensive(Pokemon p){return (int)p.getMoveset().stream().filter(a->a.getAttackCategory()!=AttackCategory.STATUS).count();}
    public static boolean ensure(Pokemon p,String name){
        return ensure(p,name,Collections.emptySet());
    }
    public static boolean ensure(Pokemon p,String name,Set<String> protectedMoves){
        if(p.getMoveset().stream().anyMatch(a->a.getMove().getAttackName().equals(name)))return true;
        if(!legal(p,name))return false;var attack=new Attack(name);int slot=-1;double weakest=Double.MAX_VALUE;
        for(int i=0;i<p.getMoveset().size();i++){
            var old=p.getMoveset().get(i);if(KEEP.contains(old.getMove().getAttackName())||protectedMoves.contains(old.getMove().getAttackName()))continue;
            boolean status=old.getAttackCategory()==AttackCategory.STATUS;
            if(attack.getAttackCategory()==AttackCategory.STATUS&&!status&&offensive(p)<=2)continue;
            double value=status?-100:old.getMove().getBasePower()*(p.getForm().getTypes().contains(old.getType())?1.5:1);
            if(value<weakest){weakest=value;slot=i;}
        }
        if(p.getMoveset().size()<4)p.getMoveset().add(attack);else if(slot>=0)p.getMoveset().set(slot,attack);else return false;
        return true;
    }
    public static void configure(List<Pokemon> team){
        var assigned=new HashSet<Pokemon>();
        Pokemon primary=team.stream().filter(p->(capabilities(p.getForm())&3)==3).max(Comparator.comparingDouble(p->role(p,"Haze",Collections.emptySet())+role(p,"Snatch",Collections.emptySet())+((capabilities(p.getForm())&4)!=0?500:0)+(p.getForm().getTypes().contains(Element.DARK)?1000:0))).orElse(null);
        if(primary!=null){ensure(primary,"Haze");ensure(primary,"Snatch");ensure(primary,"Sucker Punch");assigned.add(primary);}
        for(String move:primary==null?List.of("Haze","Snatch","Sucker Punch"):List.of("Sucker Punch")){
            var options=new ArrayList<>(team);options.sort(Comparator.comparingDouble((Pokemon p)->role(p,move,assigned)).reversed());
            for(var p:options)if(legal(p,move)&&ensure(p,move)){assigned.add(p);break;}
        }
        for(var p:team)if(legal(p,"Spectral Thief")){ensure(p,"Spectral Thief");ensure(p,"Shadow Sneak");}
    }
    private static double role(Pokemon p,String move,Set<Pokemon> assigned){
        var base=p.getForm().getBattleStats();double score=base.getStat(BattleStatsType.SPEED);
        if(move.equals("Haze"))score+=base.getStat(BattleStatsType.HP)+base.getStat(BattleStatsType.SPECIAL_DEFENSE);
        if(move.equals("Snatch"))score+=base.getStat(BattleStatsType.SPECIAL_ATTACK);
        if(move.equals("Sucker Punch"))score+=base.getStat(BattleStatsType.ATTACK)+(p.getForm().getTypes().contains(Element.DARK)?100:0);
        return score-(assigned.contains(p)?130:0);
    }
}
