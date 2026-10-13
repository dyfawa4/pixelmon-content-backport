package local.pixelmon.backport;

import java.util.*;
import java.util.function.ToDoubleFunction;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.battles.attacks.*;

/** Build a trained ace's legal moves once, without modifying levels, IVs or EVs. */
public final class NpcMoves {
    private NpcMoves(){}
    public static final String ACE="BackportNpcAce";
    private static final String VERSION="BackportNpcMovesVersion";
    private static final Set<String> RECOVERY=new HashSet<>(Arrays.asList("Recover","Roost","Slack Off","Soft-Boiled","Milk Drink","Shore Up","Synthesis","Moonlight","Morning Sun"));
    private static final Set<String> RISKY=new HashSet<>(Arrays.asList("Explosion","Self-Destruct","Final Gambit","Hyper Beam","Giga Impact","Eternabeam","Solar Beam","Solar Blade","Fly","Dig","Dive","Phantom Force","Shadow Force","Focus Punch","Dream Eater","Snore","Belch","Last Resort","Future Sight","Doom Desire","Fling","Natural Gift","Synchronoise"));
    public static boolean recovery(Attack attack){return attack!=null && RECOVERY.contains(attack.getMove().getAttackName());}
    /** Keep the existing ace/utility/STAB plan. Replace at most one redundant attack. */
    public static void coverage(Pokemon p){
        if(p.getPersistentData().func_74767_n(ACE) || p.getPersistentData().func_74767_n("BackportCoverage13"))return;
        p.getPersistentData().func_74757_a("BackportCoverage13",true);
        List<Element> types=p.getForm().getTypes();List<Element> weak=new ArrayList<>();
        for(Element type:Tera.TYPES)if(type!=null && Element.getTotalEffectiveness(types,type)>1)weak.add(type);
        if(weak.isEmpty())return;
        List<Attack> attacks=new ArrayList<>();for(Attack a:p.getMoveset())attacks.add(a);
        int slot=-1;double lowest=Double.MAX_VALUE;
        for(int i=0;i<attacks.size();i++){
            Attack a=attacks.get(i);if(a.getAttackCategory()==AttackCategory.STATUS)continue;
            long same=attacks.stream().filter(b->b.getAttackCategory()!=AttackCategory.STATUS && b.getType()==a.getType()).count();
            if(same<2)continue;
            if(a.getMove().getBasePower()<lowest){lowest=a.getMove().getBasePower();slot=i;}
        }
        if(slot<0)return;
        final int replace=slot;
        boolean physical=p.getForm().getBattleStats().getStat(BattleStatsType.ATTACK)>=p.getForm().getBattleStats().getStat(BattleStatsType.SPECIAL_ATTACK);
        double best=0;ImmutableAttack selected=null;
        for(ImmutableAttack a:p.getForm().getMoves().getAllMoves()){
            if(types.contains(a.getAttackType()) || attacks.stream().anyMatch(b->b.getMove().getAttackName().equals(a.getAttackName())))continue;
            double gain=0;
            for(Element w:weak){
                double before=0;for(int i=0;i<attacks.size();i++)if(i!=replace && attacks.get(i).getAttackCategory()!=AttackCategory.STATUS)
                    before=Math.max(before,Element.getTotalEffectiveness(Collections.singletonList(w),attacks.get(i).getType()));
                gain+=Math.max(0,Element.getTotalEffectiveness(Collections.singletonList(w),a.getAttackType())-before);
            }
            double score=gain*offense(p,a,physical);
            if(score>best){best=score;selected=a;}
        }
        if(selected!=null){attacks.set(slot,new Attack(selected));p.getMoveset().clear();for(Attack a:attacks)p.getMoveset().add(a);}
    }
    public static boolean viable(com.pixelmonmod.pixelmon.api.pokemon.species.Species species){
        List<ImmutableAttack> moves=species.getDefaultForm().getMoves().getAllMoves();
        return moves.size()>=4 && moves.stream().filter(a->a.getAttackCategory()!=AttackCategory.STATUS && a.getBasePower()>0 && !RISKY.contains(a.getAttackName())).count()>=2;
    }
    public static void tune(Pokemon pokemon,UUID trainer){
        Set<String> names=new HashSet<>();boolean legal=true,stab=false;int offensive=0;
        for(Attack attack:pokemon.getMoveset()){
            legal&=pokemon.getForm().getMoves().canLearn(attack.getMove()) && names.add(attack.getMove().getAttackName());
            if(attack.getAttackCategory()!=AttackCategory.STATUS && !RISKY.contains(attack.getMove().getAttackName())){
                offensive++;stab|=pokemon.getForm().getTypes().contains(attack.getType());
            }
        }
        if(pokemon.getSpecies().getDex()==892)legal &= names.contains(pokemon.getForm().isForm("rapidstrike")?"Surging Strikes":"Wicked Blow");
        long utility=pokemon.getMoveset().stream().filter(a->a.getAttackCategory()==AttackCategory.STATUS).count();
        boolean sustain=pokemon.getMoveset().stream().anyMatch(a->recovery(a) || a.getMove().getAttackName().equals("Rest"));
        boolean support=utility>=2 && sustain && pokemon.getForm().getBattleStats().getStat(BattleStatsType.HP)+pokemon.getForm().getBattleStats().getStat(BattleStatsType.DEFENSE)+pokemon.getForm().getBattleStats().getStat(BattleStatsType.SPECIAL_DEFENSE)>=270;
        if(legal && names.size()==4 && (offensive>=2 && stab || support && offensive>=1))return;
        pokemon.getPersistentData().func_74757_a(ACE,true);pokemon.getPersistentData().func_74768_a(VERSION,0);configure(pokemon,trainer);
    }
    public static void configure(Pokemon p,UUID trainer){
        if(!p.getPersistentData().func_74767_n(ACE) || p.getPersistentData().func_74762_e(VERSION)>=1)return;
        List<ImmutableAttack> pool=new ArrayList<>(p.getForm().getMoves().getAllMoves());
        pool.sort(Comparator.comparing(ImmutableAttack::getAttackName));
        List<ImmutableAttack> chosen=new ArrayList<>(4);
        Random random=new Random(trainer.getLeastSignificantBits() ^ p.getUUID().getMostSignificantBits());
        if(p.getSpecies().getDex()==890){
            for(String name:new String[]{"Dynamax Cannon","Sludge Bomb","Flamethrower","Recover"})
                pool.stream().filter(a->a.getAttackName().equals(name)).findFirst().ifPresent(chosen::add);
        }
        if(p.getSpecies().getDex()==892){
            boolean rapid=p.getForm().isForm("rapidstrike");
            for(String name:new String[]{rapid?"Surging Strikes":"Wicked Blow","Close Combat","U-turn",rapid?"Aqua Jet":"Sucker Punch"})
                pool.stream().filter(a->a.getAttackName().equals(name)).findFirst().ifPresent(chosen::add);
        }
        boolean physical=p.getForm().getBattleStats().getStat(BattleStatsType.ATTACK)>=p.getForm().getBattleStats().getStat(BattleStatsType.SPECIAL_ATTACK);
        List<Element> types=p.getForm().getTypes();
        if(chosen.isEmpty()){
            for(Element type:types){
                ImmutableAttack move=pick(pool,chosen,a->a.getAttackType()==type?offense(p,a,physical):0,random);
                if(move!=null)chosen.add(move);
            }
            ImmutableAttack coverage=pick(pool,chosen,a->{
                double score=offense(p,a,physical);if(score<=0 || types.contains(a.getAttackType()))return 0;
                double gain=0;
                for(int i=0;i<18;i++){
                    Element target=Tera.TYPES[i];double before=0;
                    for(ImmutableAttack selected:chosen)before=Math.max(before,Element.getTotalEffectiveness(Collections.singletonList(target),selected.getAttackType()));
                    double threat=Element.getTotalEffectiveness(types,target);
                    gain+=Math.max(0,Element.getTotalEffectiveness(Collections.singletonList(target),a.getAttackType())-before)*(threat>1?2.5:1);
                }
                return score*(1+gain/8);
            },random);
            if(coverage!=null)chosen.add(coverage);
            int bulk=p.getForm().getBattleStats().getStat(BattleStatsType.HP)
                +p.getForm().getBattleStats().getStat(BattleStatsType.DEFENSE)+p.getForm().getBattleStats().getStat(BattleStatsType.SPECIAL_DEFENSE);
            boolean sustain=bulk>=290 || random.nextInt(3)==0;
            ImmutableAttack utility=pick(pool,chosen,a->utility(a,physical,sustain),random);
            if(utility!=null && chosen.size()<4)chosen.add(utility);
        }
        while(chosen.size()<4){
            ImmutableAttack move=pick(pool,chosen,a->offense(p,a,physical),random);
            if(move==null)break;chosen.add(move);
        }
        if(!chosen.isEmpty()){
            p.getMoveset().clear();for(ImmutableAttack attack:chosen)p.getMoveset().add(new Attack(attack));
        }
        p.getPersistentData().func_74768_a(VERSION,1);
    }
    private static ImmutableAttack pick(List<ImmutableAttack> pool,List<ImmutableAttack> chosen,ToDoubleFunction<ImmutableAttack> weight,Random random){
        double best=0;List<ImmutableAttack> options=new ArrayList<>();
        for(ImmutableAttack a:pool)if(!chosen.contains(a))best=Math.max(best,weight.applyAsDouble(a));
        if(best<=0)return null;
        for(ImmutableAttack a:pool)if(!chosen.contains(a) && weight.applyAsDouble(a)>=best*.94)options.add(a);
        return options.get(random.nextInt(options.size()));
    }
    private static double offense(Pokemon p,ImmutableAttack a,boolean physical){
        if(a.getAttackCategory()==AttackCategory.STATUS || a.getBasePower()<=0 || RISKY.contains(a.getAttackName()))return 0;
        double accuracy=a.getAccuracy()<1?1:Math.min(100,a.getAccuracy())/100d;
        int attack=p.getForm().getBattleStats().getStat(BattleStatsType.ATTACK),special=p.getForm().getBattleStats().getStat(BattleStatsType.SPECIAL_ATTACK);
        double category=a.getAttackCategory()==AttackCategory.PHYSICAL?(double)attack/Math.max(attack,special):(double)special/Math.max(attack,special);
        return Math.min(130,a.getBasePower())*accuracy*category+(a.getAttackName().equals("Extreme Speed")?20:0);
    }
    private static double utility(ImmutableAttack a,boolean physical,boolean sustain){
        if(a.getAttackCategory()!=AttackCategory.STATUS)return 0;
        String name=a.getAttackName();
        if(RECOVERY.contains(name))return sustain?100:65;
        if(physical && (name.equals("Swords Dance") || name.equals("Dragon Dance") || name.equals("Bulk Up")))return sustain?65:100;
        if(!physical && (name.equals("Nasty Plot") || name.equals("Calm Mind") || name.equals("Quiver Dance")))return sustain?65:100;
        return name.equals("Stealth Rock") || name.equals("Thunder Wave")?40:0;
    }
}
