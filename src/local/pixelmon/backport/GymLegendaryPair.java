package local.pixelmon.backport;
import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.species.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
/** Exactly two themed legendary slots for ordinary equal-level leaders, within six members. */
public final class GymLegendaryPair {
    private static int total(Stats form){int n=0;for(var stat:BattleStatModifiers.STATS)n+=form.getBattleStats().getStat(stat);return n;}
    private static final class Plan {final Species species;final Stats form;final String item;Plan(JsonObject row){species=PixelmonSpecies.fromNationalDex(row.get("dex").getAsInt());form=species==null?null:row.has("form")?species.getForms().stream().filter(f->f.isForm(row.get("form").getAsString())).findFirst().orElse(null):species.getDefaultForm();item=row.has("item")?row.get("item").getAsString():null;}}
    public static void ensure(TrainerParticipant side){
        var npc=side.trainer;if(AuthorBossTeams.is(npc)||!NpcGimmicks.enabled(npc)||!GymCities.isLeader(npc)||side.allPokemon.length==0)return;
        Element theme=GymCities.theme(npc);String key=theme.name().toLowerCase(Locale.ROOT);List<Plan> plans=new ArrayList<>();
        for(var row:GymAcePolicy.DATA.getAsJsonArray(key)){Plan p=new Plan(row.getAsJsonObject());if(p.form!=null&&p.form.getTypes().contains(theme)&&GymAcePolicy.strong(p.species.getDex(),total(p.form),AuthorBossTeams.legendary(p.species),p.form.getMoves().getAllMoves().stream().filter(m->m.getAttackCategory()!=com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS&&m.getBasePower()>0).count()>=2))plans.add(p);}
        Plan first=null,second=null;double best=-1e9;for(Plan a:plans)for(Plan b:plans)if(a.species!=b.species){
            double score=total(a.form)+total(b.form);boolean ap=a.form.getBattleStats().getStat(BattleStatsType.ATTACK)>a.form.getBattleStats().getStat(BattleStatsType.SPECIAL_ATTACK),bp=b.form.getBattleStats().getStat(BattleStatsType.ATTACK)>b.form.getBattleStats().getStat(BattleStatsType.SPECIAL_ATTACK);if(ap!=bp)score+=45;
            for(var type:Tera.TYPES)if(type!=Element.MYSTERY){double x=Element.getTotalEffectiveness(a.form.getTypes(),type),y=Element.getTotalEffectiveness(b.form.getTypes(),type);if(x>1&&y>1)score-=18;if(x==0||y==0)score+=4;}
            if(score>best){best=score;first=a;second=b;}}
        if(first==null)throw new IllegalStateException("Two distinct legal legendary aces unavailable: "+theme);
        // Retain the established 90% Eternatus / 10% Pecharunt poison ace policy.
        if(theme==Element.POISON){int dex=GymAcePolicy.specialPoisonAce(npc.func_110124_au())?890:1025;Plan fixed=plans.stream().filter(p->p.species.getDex()==dex).findFirst().orElse(null);if(fixed!=null){first=fixed;second=plans.stream().filter(p->p.species!=fixed.species).max(Comparator.comparingInt(p->total(p.form))).orElseThrow();}}
        List<Pokemon> regular=new ArrayList<>();for(var member:side.allPokemon)if(!AuthorBossTeams.legendary(member.pokemon.getSpecies())&&!member.pokemon.getPersistentData().func_74767_n(GymAcePolicy.MARKER))regular.add(member.pokemon);
        int level=side.allPokemon[0].pokemon.getPokemonLevel();List<Pokemon> team=new ArrayList<>(regular.subList(0,Math.min(4,regular.size())));
        if(team.size()<4){List<Species> fill=new ArrayList<>();for(int dex=1;dex<=1025;dex++){Species species=PixelmonSpecies.fromNationalDex(dex);if(species!=null&&!AuthorBossTeams.legendary(species)&&species.getDefaultForm().getTypes().contains(theme)&&species.getDefaultForm().getEvolutions().isEmpty()&&species.getDefaultForm().getMoves().getAllMoves().stream().filter(m->m.getBasePower()>0).count()>=2)fill.add(species);}fill.sort(Comparator.comparingInt((Species s)->total(s.getDefaultForm())).reversed());for(Species species:fill){if(team.stream().anyMatch(p->p.getSpecies()==species))continue;Pokemon p=PokemonFactory.create(species);p.setLevelNum(level);p.initialize();p.rerollMoveset();team.add(p);if(team.size()==4)break;}}
        if(team.size()!=4)throw new IllegalStateException("Four ordinary gym slots unavailable: "+theme);
        for(Plan plan:List.of(first,second)){Pokemon p=PokemonFactory.create(plan.species);p.setForm(plan.form);p.setLevelNum(level);p.initialize();p.rerollMoveset();if(plan.item!=null)p.setHeldItem(AuthorBossTeams.item(plan.item));p.setForm(plan.form);p.getPersistentData().func_74757_a(GymAcePolicy.MARKER,true);p.getPersistentData().func_74757_a(NpcMoves.ACE,true);NpcMoves.configure(p,npc.func_110124_au());NpcBuildRoles.configure(p);NpcTraining.maximize(p);p.heal();team.add(p);}
        // Repeated start hooks retain the same six-slot layout; no extra author power slots.
        side.allPokemon=new PixelmonWrapper[team.size()];for(int i=0;i<team.size();i++){npc.getPokemonStorage().set(i,team.get(i));var wrapper=new PixelmonWrapper(side,team.get(i),i);wrapper.bc=side.bc;side.allPokemon[i]=wrapper;}
        npc.getPersistentData().func_74768_a("BackportNpcRosterVersion",3);
    }
}
