package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;

/** Author-only six-slot builds. Theme identifies fixed members, never a type restriction. */
public final class AuthorRoster {
    public static final String VERSION="BackportAuthorRoster180",MEMBER="BackportAuthorBuild180";
    private static final int[] STRONG={CapSpecies.DEX,493,658,890,898,898};
    private static final BattleStatsType[] STATS={BattleStatsType.HP,BattleStatsType.ATTACK,BattleStatsType.DEFENSE,BattleStatsType.SPECIAL_ATTACK,BattleStatsType.SPECIAL_DEFENSE,BattleStatsType.SPEED};
    private AuthorRoster(){}
    private static Pokemon make(int dex,String form,String item,Random random){
        Species species=PixelmonSpecies.fromNationalDex(dex);
        if(species==null)throw new IllegalStateException("Missing author species "+dex);
        Pokemon p=PokemonFactory.create(species);if(form!=null&&!form.isEmpty()){if(species.getForms().stream().noneMatch(f->f.isForm(form)))throw new IllegalStateException("Missing author form "+dex+"/"+form);p.setForm(form);}
        p.setLevelNum(70);p.initialize();p.rerollMoveset();
        if(item!=null)p.setHeldItem(AuthorBossTeams.item(item));
        p.getPersistentData().func_74757_a(MEMBER,true);p.getPersistentData().func_74757_a(NpcMoves.ACE,true);
        NpcMoves.configure(p,new UUID(random.nextLong(),random.nextLong()));
        NpcBuildRoles.configure(p);
        p.getPersistentData().func_74757_a(NpcGimmicks.PROFILE,true);return p;
    }
    public static Pokemon strong(int index,Random random){
        switch(index){
            case 0:return make(STRONG[index],"","pixelmon:pikashunium_z",random);
            case 1:return make(493,"","pixelmon:legend_plate",random);
            case 2:return make(658,PermanentAsh.MEGA,Content.NS+":ash_greninjite",random);
            case 3:return make(890,"eternamax",Content.NS+":eternatus_meteorite",random);
            case 4:return make(898,"shadowrider",null,random);
            case 5:return make(898,"icerider",null,random);
            default:throw new IllegalArgumentException("Strong slot "+index);
        }
    }
    private static int total(Species s){int total=0;for(var stat:STATS)total+=s.getDefaultForm().getBattleStats().getStat(stat);return total;}
    private static List<Species> candidates(boolean legends){
        List<Species> result=new ArrayList<>();
        for(Species s:PixelmonSpecies.getAll())if(s.getDex()>0&&s.getDex()!=CapSpecies.DEX
            &&AuthorBossTeams.legendary(s)==legends&&s.getDefaultForm().getEvolutions().isEmpty()
            &&total(s)>=(legends?570:480)&&NpcMoves.viable(s))result.add(s);
        result.sort(Comparator.comparingInt(Species::getDex));return result;
    }
    private static boolean present(List<Pokemon> team,Species s){return team.stream().anyMatch(p->p.getSpecies().getDex()==s.getDex());}
    private static double effectiveness(List<Element> types,String ability,Element attack){
        if(attack==Element.GROUND&&ability.equals("Levitate")||attack==Element.ELECTRIC&&Set.of("VoltAbsorb","MotorDrive","LightningRod").contains(ability)
            ||attack==Element.WATER&&Set.of("WaterAbsorb","StormDrain","DrySkin","DesolateLand").contains(ability)
            ||attack==Element.FIRE&&Set.of("FlashFire","PrimordialSea").contains(ability)||attack==Element.GRASS&&ability.equals("SapSipper"))return 0;
        return Element.getTotalEffectiveness(types,attack);
    }
    private static double[] defences(Pokemon p){double[] values=new double[18];String ability=p.getAbility()==null?"":p.getAbility().getClass().getSimpleName();for(int i=0;i<18;i++)values[i]=effectiveness(p.getForm().getTypes(),ability,Tera.TYPES[i]);return values;}
    private static double[] defences(Species s){
        double[] values=new double[18];for(int i=0;i<18;i++)values[i]=Element.getTotalEffectiveness(s.getDefaultForm().getTypes(),Tera.TYPES[i]);return values;
    }
    private static double score(List<double[]> team,double[] first,double[] second){
        double result=0;for(int i=0;i<18;i++){
            double best=Math.min(first[i],second==null?4:second[i]);int weak=0,resist=best<1?1:0;
            for(double[] mon:team){best=Math.min(best,mon[i]);if(mon[i]>1)weak++;if(mon[i]<1)resist++;}
            result+=(best==0?18:best<1?12:best==1?0:-90)*(1+weak*.4)+Math.min(2,resist)*2;
            if((Tera.TYPES[i]==Element.ELECTRIC||Tera.TYPES[i]==Element.GRASS)&&best>=1)result-=20;
        }return result;
    }
    private static void cover(List<Pokemon> team,Random random){
        boolean legendaryFirst=random.nextInt(100)<80,legendarySecond=random.nextInt(100)<80;
        List<Species> first=new ArrayList<>(candidates(legendaryFirst)),second=new ArrayList<>(candidates(legendarySecond));
        first.removeIf(s->present(team,s));second.removeIf(s->present(team,s));
        if(first.isEmpty()||second.isEmpty())throw new IllegalStateException("No author defensive candidates");
        int control=GymCounterTeam.capabilities(team);List<double[]> base=new ArrayList<>();for(Pokemon p:team)base.add(defences(p));
        Map<Species,double[]> cache=new IdentityHashMap<>();for(var s:first)cache.put(s,defences(s));for(var s:second)cache.put(s,defences(s));
        boolean complete=false;for(var x:first)for(var y:second)if(x!=y&&((control|GymCounterTeam.capabilities(x.getDefaultForm())|GymCounterTeam.capabilities(y.getDefaultForm()))&31)==31){complete=true;break;}
        Species a=null,b=null;double best=-Double.MAX_VALUE;
        for(Species x:first)for(Species y:second){if(x==y)continue;if(complete&&((control|GymCounterTeam.capabilities(x.getDefaultForm())|GymCounterTeam.capabilities(y.getDefaultForm()))&31)!=31)continue;
            double value=GymCounterTeam.coverage(control|GymCounterTeam.capabilities(x.getDefaultForm())|GymCounterTeam.capabilities(y.getDefaultForm()))+score(base,cache.get(x),cache.get(y))+(total(x)+total(y))*.005+random.nextDouble()*4;
            if(team.get(0).getSpecies().getDex()==382){int electric=Arrays.asList(Tera.TYPES).indexOf(Element.ELECTRIC);double bestElectric=Math.min(cache.get(x)[electric],cache.get(y)[electric]);for(var defence:base)bestElectric=Math.min(bestElectric,defence[electric]);if(bestElectric>0)value-=150;}
            if(value>best){best=value;a=x;b=y;}
        }
        if(a==null)throw new IllegalStateException("No author defensive pair");
        team.add(make(a.getDex(),"",null,random));team.add(make(b.getDex(),"",null,random));
    }
    public static void support(List<Pokemon> team){
        
        for(Pokemon p:team){
            if(p.getSpecies().getDex()==CapSpecies.DEX)ensure(p,"Ten Million Volts");
            if(p.getSpecies().getDex()==658&&PermanentAsh.isAsh(p.getForm()))ensure(p,"Water Shuriken");

        }
        GymCounterTeam.configure(team);
    }
    private static void ensure(Pokemon p,String name){
        if(p.getMoveset().stream().anyMatch(a->a.getMove().getAttackName().equals(name)))return;
        Attack attack=new Attack(name);int replace=p.getMoveset().size()-1;
        for(int i=0;i<p.getMoveset().size();i++){Attack a=p.getMoveset().get(i);if(a.getAttackCategory()==AttackCategory.STATUS){replace=i;break;}}
        if(p.getMoveset().size()<4)p.getMoveset().add(attack);else {if(Set.of("Ten Million Volts","Water Shuriken").contains(p.getMoveset().get(replace).getMove().getAttackName()))for(int i=0;i<4;i++)if(!Set.of("Ten Million Volts","Water Shuriken").contains(p.getMoveset().get(i).getMove().getAttackName())){replace=i;break;}p.getMoveset().set(replace,attack);}
    }
    public static void train(Pokemon p){
        NpcTraining.prepare(p);for(var stat:STATS){p.getIVs().setStat(stat,31);p.getIVs().setHyperTrained(stat,false);}
        p.getEVs().fillFromArray(252,252,252,252,252,252);p.getStats().recalculateStats();
    }
    public static List<Pokemon> build(String theme,Random random){
        List<Pokemon> team=new ArrayList<>();
        if(theme.equals("plains"))team.add(make(493,"","pixelmon:legend_plate",random));
        if(theme.equals("ocean"))team.add(make(382,"primal","pixelmon:blue_orb",random));
        if(theme.equals("desert"))team.add(make(383,"primal","pixelmon:red_orb",random));
        if(theme.equals("swamp"))team.add(make(890,"eternamax",Content.NS+":eternatus_meteorite",random));
        while(team.size()<2){var core=AuthorCorePool.choose(team,random);team.add(make(core.dex,core.form,core.item,random));}
        team.add(strong(random.nextInt(6),random));team.add(strong(random.nextInt(6),random));
        if(theme.equals("badlands")){
            // The pre-existing guaranteed cap Pikachu occupies a remaining support slot.
            team.add(make(CapSpecies.DEX,"","pixelmon:pikashunium_z",random));
            var options=candidates(random.nextInt(100)<80);options.removeIf(s->present(team,s));List<double[]> base=new ArrayList<>();for(Pokemon p:team)base.add(defences(p));
            var controlled=new ArrayList<Species>();for(var s:options)if(((GymCounterTeam.capabilities(team)|GymCounterTeam.capabilities(s.getDefaultForm()))&31)==31)controlled.add(s);if(!controlled.isEmpty())options=controlled;
            Species best=options.stream().max(Comparator.comparingDouble(s->GymCounterTeam.coverage(GymCounterTeam.capabilities(team)|GymCounterTeam.capabilities(s.getDefaultForm()))+score(base,defences(s),null))).orElseThrow();team.add(make(best.getDex(),"",null,random));
        }else cover(team,random);
        support(team);boolean mega=false;
        for(Pokemon p:team){
            if(p.getHeldItem().func_190926_b()&&!mega){
                for(var stone:com.pixelmonmod.pixelmon.api.registries.PixelmonItems.megaStones)if(com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper.canMegaEvolve(new net.minecraft.item.ItemStack(stone),p.getSpecies(),p.getForm().getName())){p.setHeldItem(new net.minecraft.item.ItemStack(stone));mega=true;break;}
            }
            train(p);NpcHeldItems.equip(p);p.heal();
        }return team;
    }
    public static void upgrade(NPCTrainer npc){
        if(!AuthorBossTeams.is(npc)||npc.getPersistentData().func_74762_e(VERSION)>=3||npc.battleController!=null&&!npc.battleController.battleEnded)return;
        String theme=npc.getPersistentData().func_74779_i("AuthorBossTheme");if(theme.isEmpty())return;
        List<Pokemon> roster=build(theme,new Random(npc.func_110124_au().getMostSignificantBits()^npc.func_110124_au().getLeastSignificantBits()));
        for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,roster.get(i));npc.getPersistentData().func_74768_a(VERSION,3);
    }
}
