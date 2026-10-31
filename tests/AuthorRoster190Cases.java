package qa;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import local.pixelmon.backport.*;

/** Private native roster, targeted migration and uncursed preview audit. */
public final class AuthorRoster190Cases {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static int total(Pokemon p){return Arrays.stream(Calamity.STATS).mapToInt(s->p.getForm().getBattleStats().getStat(s)).sum();}
    static String identity(Pokemon p){return p.getUUID()+"/"+p.getSpecies().getDex()+"/"+p.getForm().getName()+"/"+p.getMoveset()+"/"+p.getHeldItem();}
    static boolean has(Pokemon p,String... moves){return p.getMoveset().stream().anyMatch(a->Arrays.asList(moves).contains(a.getMove().getAttackName()));}
    static void roster(List<Pokemon> team,String theme){
        check(team.size()==6,"Six slots "+theme);Set<Integer> cores=new HashSet<>();
        for(int i=0;i<6;i++){
            var p=team.get(i);check(Arrays.stream(p.getEVs().getArray()).allMatch(v->v==252),"Author six EV252");check(Arrays.stream(p.getIVs().getArray()).allMatch(v->v==31),"Author IV31");
            check(p.getMoveset().size()==4,"Four moves "+p.getSpecies().getName());
            check(p.getMoveset().stream().filter(a->a.getAttackCategory()!=AttackCategory.STATUS&&a.getMove().getBasePower()>0).count()>=(AuthorSupportPlan.support(p)?1:2),"Role-appropriate damaging moves "+p.getSpecies().getName()+p.getMoveset());
            for(var a:p.getMoveset())check(p.getForm().getMoves().getAllMoves().stream().anyMatch(m->m.getAttackName().equals(a.getMove().getAttackName()))||p.getSpecies().getDex()==CapSpecies.DEX&&a.getMove().getAttackName().equals("Ten Million Volts"),"Legal author move "+p.getSpecies().getName()+" "+a);
            if(i<3){check(total(p)>=650,"Actual core BST650 "+p.getSpecies().getName());check(AuthorBossTeams.legendary(p.getSpecies()),"Legendary core");check(cores.add(p.getSpecies().getDex()),"Distinct core species");}
            if(i==3||i==4)check(Set.of(CapSpecies.DEX,493,658,890,898).contains(p.getSpecies().getDex()),"Two unchanged strong families");
        }
        if(theme.equals("plains"))check(team.get(0).getSpecies().getDex()==493,"Fixed Arceus");
        if(theme.equals("ocean"))check(team.get(0).getSpecies().getDex()==382&&team.get(0).getForm().isForm("primal"),"Fixed Primal Kyogre");
        if(theme.equals("desert"))check(team.get(0).getSpecies().getDex()==383&&team.get(0).getForm().isForm("primal"),"Fixed Primal Groudon");
        if(theme.equals("swamp"))check(team.get(0).getSpecies().getDex()==890&&team.get(0).getForm().isForm("eternamax"),"Fixed Eternamax");
        if(theme.equals("badlands"))check(team.get(5).getSpecies().getDex()==CapSpecies.DEX,"Fixed Cap sixth slot");
        else check((GymCounterTeam.capabilities(team)&31)==31,"Control pool coverage "+theme);
    }
    public static void run(ServerPlayerEntity player)throws Exception{run(player,true);}
    public static void run(ServerPlayerEntity player,boolean verifyLegacy)throws Exception{
        List<String> themes=new ArrayList<>(AuthorBossPolicy.TYPES.keySet());themes.addAll(List.of("ultra","paradox"));
        JsonArray rows=new JsonArray();Map<Integer,Integer> coreCounts=new TreeMap<>();Set<String> combinations=new HashSet<>();int recovery=0,physical=0,special=0,boost=0;
        for(String theme:themes)for(int seed=0;seed<16;seed++){
            var team=AuthorRoster.build(theme,new Random(1904000L+seed));roster(team,theme);
            JsonObject row=new JsonObject();row.addProperty("theme",theme);row.addProperty("seed",seed);JsonArray members=new JsonArray();
            for(var p:team)members.add(Conventional190Matches.row(p));row.add("members",members);rows.add(row);
            for(var p:team.subList(0,3))coreCounts.merge(p.getSpecies().getDex(),1,Integer::sum);
            combinations.add(team.subList(0,3).stream().map(p->p.getSpecies().getDex()+"/"+p.getForm().getName()).sorted().collect(java.util.stream.Collectors.joining(",")));
            if(team.subList(0,3).stream().anyMatch(p->has(p,"Recover","Roost","Morning Sun","Oblivion Wing","Rest")))recovery++;
            if(team.stream().anyMatch(p->p.getMoveset().stream().anyMatch(a->a.getAttackCategory()==AttackCategory.PHYSICAL&&a.getMove().getBasePower()>0)))physical++;
            if(team.stream().anyMatch(p->p.getMoveset().stream().anyMatch(a->a.getAttackCategory()==AttackCategory.SPECIAL&&a.getMove().getBasePower()>0)))special++;
            if(team.stream().anyMatch(p->has(p,"Swords Dance","Calm Mind","Dragon Dance","Nasty Plot","Geomancy","Trick Room")))boost++;
        }
        check(combinations.size()>=12,"Varied strong cores "+combinations.size());
        check(recovery>rows.size()/2,"Cores participate in sustain");
        check(physical==rows.size()&&special==rows.size(),"Mixed offensive pressure physical="+physical+" special="+special);
        check(boost==rows.size(),"Breakthrough plans retained "+boost);
        int migrations=verifyLegacy?migrations(player):0;int stats=stats(player);int scouting=scouting(player,themes);
        JsonObject report=new JsonObject();report.addProperty("teams",rows.size());report.addProperty("core_combinations",combinations.size());report.add("core_counts",new Gson().toJsonTree(coreCounts));report.addProperty("core_recovery_teams",recovery);report.addProperty("physical_teams",physical);report.addProperty("special_teams",special);report.addProperty("breakthrough_teams",boost);report.addProperty("migrations",migrations);report.addProperty("stat_cases",stats);report.add("rows",rows);
        report.addProperty("scouting_cases",scouting);Files.writeString(Path.of("author-roster190-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
        BossBalance190Probe.log("ROSTER190_PASS teams="+rows.size()+" combinations="+combinations.size()+" recovery="+recovery+" mixed="+physical+"/"+special+" breakthrough="+boost+" migrations="+migrations+" statCases="+stats+" scouting="+scouting);
    }
    static int scouting(ServerPlayerEntity player,List<String> themes)throws Exception{
        var party=StorageProxy.getParty(player);var saved=party.getAll().clone();int cases=0,failures=0;JsonArray audit=new JsonArray();
        try{for(String theme:themes)for(int seed=0;seed<4;seed++){
            var foes=List.of(Update180Cases.mon(313,"Tail Glow","Baton Pass","Bug Buzz","Protect"),Update180Cases.mon(227,"Spikes","Stealth Rock","Roost","Brave Bird"),Update180Cases.mon(748,"Recover","Baneful Bunker","Scald","Haze"),Update180Cases.mon(488,"Trick Room","Reflect","Moonlight","Psychic"),Update180Cases.mon(36,"Moonblast","Calm Mind","Soft-Boiled","Encore"),Update180Cases.mon(598,"Power Whip","Leech Seed","Spikes","Protect"));
            for(int i=0;i<6;i++){NpcTraining.maximize(foes.get(i));party.set(i,foes.get(i));}
            var npc=new NPCTrainer(player.field_70170_p);npc.isGymLeader=true;npc.getPersistentData().func_74778_a(GymCities.TYPE,"psychic");npc.getPersistentData().func_74757_a(AuthorBossTeams.MARKER,true);npc.getPersistentData().func_74778_a("AuthorBossTheme",theme);npc.getPersistentData().func_74768_a(AuthorRoster.VERSION,AuthorRoster.CURRENT);npc.getPersistentData().func_74768_a(AuthorRoster.TRAINING,2);npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));
            var team=AuthorRoster.build(theme,new Random(1904000L+seed));for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,team.get(i));
            var human=new PlayerParticipant(player,foes.get(0));var trainer=new TrainerParticipant(npc,1);var b=new BattleController(new BattleParticipant[]{human},new BattleParticipant[]{trainer},new BattleRules());b.sendMessages=false;
            var init=BattleController.class.getDeclaredMethod("initBattle");init.setAccessible(true);init.invoke(b);
            try{
                NpcTeamKnowledge.capture(trainer);NpcTeamKnowledge.configure(trainer);
                check(NpcTeamKnowledge.plan(b,NpcTeamKnowledge.Plan.HAZARDS)&&NpcTeamKnowledge.plan(b,NpcTeamKnowledge.Plan.SETUP)&&NpcTeamKnowledge.plan(b,NpcTeamKnowledge.Plan.STALL),"Actual threat scouting");
                var members=Arrays.stream(trainer.allPokemon).map(w->w.pokemon).collect(java.util.stream.Collectors.toList());
                boolean setup=members.stream().anyMatch(p->has(p,"Haze","Clear Smog","Roar","Whirlwind")||p.getAbility().getClass().getSimpleName().equals("Unaware"));
                boolean disruption=members.stream().anyMatch(p->has(p,"Taunt","Encore"));
                JsonObject sample=new JsonObject();sample.addProperty("theme",theme);sample.addProperty("seed",seed);sample.addProperty("setup",setup);sample.addProperty("disruption",disruption);JsonArray inspected=new JsonArray();
                for(var p:members){JsonObject row=Conventional190Matches.row(p);row.add("legal_support",new Gson().toJsonTree(p.getForm().getMoves().getAllMoves().stream().map(a->a.getAttackName()).filter(n->Set.of("Taunt","Encore","Haze","Snatch","Roar","Whirlwind","Defog","Knock Off").contains(n)).collect(java.util.stream.Collectors.toList())));inspected.add(row);}sample.add("members",inspected);audit.add(sample);
                if(!setup||!disruption){failures++;BossBalance190Probe.log("ROSTER190_SCOUT_MISSING "+theme+"/"+seed+" setup="+setup+" disruption="+disruption);}
                for(var p:members)check(p.getMoveset().stream().filter(a->a.getAttackCategory()!=AttackCategory.STATUS&&a.getMove().getBasePower()>0).count()>=(AuthorSupportPlan.support(p)?1:2),"Scouting retains role-appropriate offensive pressure "+p.getSpecies().getName()+" support="+AuthorSupportPlan.support(p)+" moves="+p.getMoveset().stream().map(a->a.getMove().getAttackName()).collect(java.util.stream.Collectors.toList()));
                cases++;
            }finally{b.battleEnded=true;NpcTeamKnowledge.end(b);NpcTurnKnowledge.end(b);AuthorForesight.end(b);GymChallenge.end(b);Erosion.end(b);CalamityBattle.end(b);npc.func_70106_y();}
        }}finally{for(int i=0;i<6;i++)party.set(i,saved[i]);}
        Files.writeString(Path.of("scouting190-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(audit));check(failures==0,"Scouting lost responses cases="+failures);return cases;
    }
    static int migrations(ServerPlayerEntity player)throws Exception{
        int cases=0;
        for(String theme:List.of("forest","badlands","plains","ocean")){
            // Obtain genuinely old-layout members from the immutable previous core fixture.
            var source=new JsonParser().parse(Files.readString(Path.of("legacy-rosters.json"))).getAsJsonObject().getAsJsonArray(theme);
            List<Pokemon> old=new ArrayList<>();for(var n:source)old.add(PokemonFactory.create(net.minecraft.nbt.JsonToNBT.func_180713_a(n.getAsString())));
            var npc=new NPCTrainer(player.field_70170_p);npc.getPersistentData().func_74757_a(AuthorBossTeams.MARKER,true);npc.getPersistentData().func_74778_a("AuthorBossTheme",theme);npc.getPersistentData().func_74768_a(AuthorRoster.VERSION,3);npc.getPersistentData().func_74768_a(AuthorRoster.TRAINING,2);npc.getPersistentData().func_74778_a("AuthorArenaWins","sentinel");
            List<String> ids=new ArrayList<>();for(int i=0;i<6;i++){npc.getPokemonStorage().set(i,old.get(i));ids.add(identity(old.get(i)));}
            AuthorRoster.upgrade(npc);var team=Arrays.asList(npc.getPokemonStorage().getAll());
            check(team.size()==6&&team.stream().noneMatch(Objects::isNull),"No UUID relocation loss");
            for(int i=0;i<2;i++)check(identity(team.get(i)).equals(ids.get(i)),"Old core identity/moves/item retained");
            for(int i=2;i<4;i++)check(identity(team.get(i+1)).equals(ids.get(i)),"Old strong identity/moves/item retained");
            check(identity(team.get(5)).equals(ids.get(4))||identity(team.get(5)).equals(ids.get(5)),"One old flex retained");
            if(theme.equals("badlands"))check(identity(team.get(5)).equals(ids.get(4)),"Old guaranteed Cap retained");
            check(total(team.get(2))>=650,"New strong third core");check(npc.getPersistentData().func_74762_e(AuthorRoster.VERSION)==AuthorRoster.CURRENT,"Version4 migration");
            var after=team.stream().map(AuthorRoster190Cases::identity).collect(java.util.stream.Collectors.toList());AuthorRoster.upgrade(npc);
            check(after.equals(Arrays.stream(npc.getPokemonStorage().getAll()).map(AuthorRoster190Cases::identity).collect(java.util.stream.Collectors.toList())),"Idempotent upgrade");
            check(npc.getPersistentData().func_74779_i("AuthorArenaWins").equals("sentinel"),"Challenge records retained");
            CompoundNBT saved=npc.getPokemonStorage().writeToNBT(new CompoundNBT());check(saved!=null,"Native storage serializes");npc.func_70106_y();cases++;
        }
        return cases;
    }
    static int[] values(PixelmonWrapper p){var s=p.getBattleStats();return new int[]{p.getMaxHealth(),s.attackStat,s.defenseStat,s.specialAttackStat,s.specialDefenseStat,s.speedStat};}
    static int stats(ServerPlayerEntity player)throws Exception{
        var party=StorageProxy.getParty(player);var saved=party.getAll().clone();int cases=0;
        try{for(int dex:new int[]{143,658,6,716})for(boolean cursed:new boolean[]{false,true}){
            var mon=Update180Cases.mon(dex,"Surf","Ice Beam","Protect","Tackle");NpcTraining.maximize(mon);mon.setHeldItem(net.minecraft.item.ItemStack.field_190927_a);
            int[] uncursed=Arrays.stream(Calamity.STATS).mapToInt(mon::getStat).toArray();if(cursed)mon.getPersistentData().func_74768_a(Calamity.TAG,20);
            for(int i=0;i<6;i++)party.set(i,i==0?mon:null);
            var npc=new NPCTrainer(player.field_70170_p);npc.getPersistentData().func_74757_a(AuthorBossTeams.MARKER,true);npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,i==0?Update180Cases.mon(143,"Tackle"):null);
            var human=new PlayerParticipant(player,mon);var trainer=new TrainerParticipant(npc,1);var b=new BattleController(new BattleParticipant[]{human},new BattleParticipant[]{trainer},new BattleRules());b.sendMessages=false;
            var init=BattleController.class.getDeclaredMethod("initBattle");init.setAccessible(true);init.invoke(b);
            try{
                var live=human.controlledPokemon.get(0);Calamity.enter(live);b.modifyStats();
                check(Calamity.active(mon)==cursed&&CalamityBattle.active(live)==cursed,"Author does not add curse");
                if(!cursed)check(Arrays.equals(values(live),uncursed),"No phantom stat reduction");else check(live.getMaxHealth()==uncursed[0]-40,"Old curse reduced exactly once");
                var copy=NpcSimulationCopy.of(live);copy.onBattlefield=true;boolean previous=b.simulateMode;b.simulateMode=true;try{b.modifyStats(copy);}finally{b.simulateMode=previous;}
                check(Arrays.equals(values(live),values(copy)),"Detached preview uses actual stats "+dex+" cursed="+cursed+" "+Arrays.toString(values(live))+"/"+Arrays.toString(values(copy)));
                check(Calamity.active(mon)==cursed,"Preview leaves persistent curse alone");
                Calamity.cured(mon);b.modifyStats();check(Arrays.equals(values(live),uncursed),"Cure restores all actual stats");
                var clean=NpcSimulationCopy.of(live);clean.onBattlefield=true;b.simulateMode=true;try{b.modifyStats(clean);}finally{b.simulateMode=false;}
                check(Arrays.equals(values(live),values(clean)),"Cured preview agrees");cases++;
            }finally{b.battleEnded=true;NpcTeamKnowledge.end(b);NpcTurnKnowledge.end(b);AuthorForesight.end(b);GymChallenge.end(b);Erosion.end(b);CalamityBattle.end(b);npc.func_70106_y();}
        }}finally{for(int i=0;i<6;i++)party.set(i,saved[i]);}
        return cases;
    }
    public static void legacy()throws Exception{
        JsonObject data=new JsonObject();for(String theme:List.of("forest","badlands","plains","ocean")){JsonArray members=new JsonArray();for(var p:AuthorRoster.build(theme,new Random(190321L)))members.add(p.writeToNBT(new CompoundNBT()).toString());data.add(theme,members);}
        Files.writeString(Path.of("legacy-rosters.json"),new GsonBuilder().setPrettyPrinting().create().toJson(data));BossBalance190Probe.log("ROSTER190_LEGACY_CAPTURED native previous-core teams");
    }
}
