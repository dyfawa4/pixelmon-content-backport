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
    public static final String VERSION="BackportAuthorRoster180",MEMBER="BackportAuthorBuild180",TRAINING="BackportAuthorTraining190";
    public static final int CURRENT=4;
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
            &&total(s)>=(legends?650:480)&&NpcMoves.viable(s))result.add(s);
        result.sort(Comparator.comparingInt(Species::getDex));return result;
    }
    private static boolean present(List<Pokemon> team,Species s){return team.stream().anyMatch(p->p.getSpecies().getDex()==s.getDex());}
    private static Pokemon cover(List<Pokemon> team,Random random){
        // A single flex slot may be either legendary or ordinary: select its role,
        // rather than spending an arbitrary 80% roll on a fourth legendary label.
        List<Species> options=candidates(false);options.addAll(candidates(true));options.removeIf(s->present(team,s));
        int control=GymCounterTeam.capabilities(team);double[] scores=new double[options.size()];
        for(int i=0;i<options.size();i++){
            Species s=options.get(i);var form=s.getDefaultForm();
            scores[i]=GymCounterTeam.coverage(control|GymCounterTeam.capabilities(form))+AuthorTeamBalance.score(team,form)+AuthorTeamBalance.role(team,form)+total(s)*.005;
        }
        int selected=NearBest.choose(scores,30,12,random);
        if(selected<0)throw new IllegalStateException("No author defensive member");
        return make(options.get(selected).getDex(),"",null,random);
    }
    private static double keepScore(List<Pokemon> core,Pokemon candidate){
        return GymCounterTeam.coverage(GymCounterTeam.capabilities(core)|GymCounterTeam.capabilities(candidate.getForm()))+AuthorTeamBalance.score(core,candidate);
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
        NpcTraining.maximize(p);
        p.getEVs().fillFromArray(252,252,252,252,252,252);
        p.getStats().recalculateStats();
    }
    public static List<Pokemon> build(String theme,Random random){
        List<Pokemon> team=new ArrayList<>();
        if(theme.equals("plains"))team.add(make(493,"","pixelmon:legend_plate",random));
        if(theme.equals("ocean"))team.add(make(382,"primal","pixelmon:blue_orb",random));
        if(theme.equals("desert"))team.add(make(383,"primal","pixelmon:red_orb",random));
        if(theme.equals("swamp"))team.add(make(890,"eternamax",Content.NS+":eternatus_meteorite",random));
        List<Pokemon> strong=List.of(strong(random.nextInt(6),random),strong(random.nextInt(6),random));
        Pokemon fixed=theme.equals("badlands")?make(CapSpecies.DEX,"","pixelmon:pikashunium_z",random):null;
        List<Pokemon> context=new ArrayList<>(team);context.addAll(strong);if(fixed!=null)context.add(fixed);
        for(var core:AuthorCorePool.draft(context,3-team.size(),random))team.add(make(core.dex,core.form,core.item,random));
        team.addAll(strong);team.add(fixed==null?cover(team,random):fixed);
        support(team);boolean mega=false;
        for(Pokemon p:team){
            if(p.getHeldItem().func_190926_b()&&!mega){
                for(var stone:com.pixelmonmod.pixelmon.api.registries.PixelmonItems.megaStones)if(com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper.canMegaEvolve(new net.minecraft.item.ItemStack(stone),p.getSpecies(),p.getForm().getName())){p.setHeldItem(new net.minecraft.item.ItemStack(stone));mega=true;break;}
            }
            train(p);NpcHeldItems.equip(p);p.heal();
        }return team;
    }
    public static void upgrade(NPCTrainer npc){
        if(!AuthorBossTeams.is(npc)||npc.battleController!=null&&!npc.battleController.battleEnded)return;
        if(npc.getPersistentData().func_74762_e(VERSION)<3){
            String theme=npc.getPersistentData().func_74779_i("AuthorBossTheme");if(theme.isEmpty())return;
            List<Pokemon> roster=build(theme,new Random(npc.func_110124_au().getMostSignificantBits()^npc.func_110124_au().getLeastSignificantBits()));
            for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,roster.get(i));npc.getPersistentData().func_74768_a(VERSION,CURRENT);
        }
        if(npc.getPersistentData().func_74762_e(VERSION)<CURRENT){
            String theme=npc.getPersistentData().func_74779_i("AuthorBossTheme");if(theme.isEmpty())return;
            var storage=npc.getPokemonStorage();List<Pokemon> old=new ArrayList<>();for(int i=0;i<6;i++){Pokemon p=storage.get(i);if(p==null)return;old.add(p);}
            // Version 3: two cores, two strong, two flex (badlands flex0 is fixed Cap).
            // Keep both cores and both strong members, and the best surviving flex.
            int keep=theme.equals("badlands")?4:keepScore(old.subList(0,4),old.get(4))>=keepScore(old.subList(0,4),old.get(5))?4:5;
            List<Pokemon> context=new ArrayList<>(old.subList(0,4));context.add(old.get(keep));
            var core=AuthorCorePool.choose(context,new Random(npc.func_110124_au().getMostSignificantBits()^npc.func_110124_au().getLeastSignificantBits()^1904L));
            Pokemon added=make(core.dex,core.form,core.item,new Random(npc.func_110124_au().getLeastSignificantBits()^1904L));train(added);NpcHeldItems.equip(added);added.heal();
            List<Pokemon> roster=List.of(old.get(0),old.get(1),added,old.get(2),old.get(3),old.get(keep));
            // Clear before rearranging; native storage enforces UUID uniqueness.
            for(int i=0;i<6;i++)storage.set(i,null);
            for(int i=0;i<6;i++)storage.set(i,roster.get(i));
            npc.getPersistentData().func_74768_a(VERSION,CURRENT);
        }
        if(npc.getPersistentData().func_74762_e(TRAINING)<2){
            for(Pokemon p:npc.getPokemonStorage().getAll())if(p!=null)train(p);
            npc.getPersistentData().func_74768_a(TRAINING,2);
        }
    }
}
