package qa;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.battles.BattleAIMode;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import local.pixelmon.backport.*;
import net.minecraft.entity.player.ServerPlayerEntity;
public final class AuthorTrial190Cases {
    static void check(boolean b,String text){if(!b)throw new AssertionError(text);}
    static int total(Pokemon p){return Arrays.stream(Calamity.STATS).mapToInt(s->p.getForm().getBattleStats().getStat(s)).sum();}
    static String identity(Pokemon p){return p.getUUID()+"/"+p.getSpecies().getDex()+"/"+p.getForm().getName()+"/"+p.getMoveset()+"/"+p.getHeldItem();}
    public static void run(ServerPlayerEntity player)throws Exception{
        List<String> themes=new ArrayList<>(AuthorBossPolicy.TYPES.keySet());themes.addAll(List.of("ultra","paradox"));int members=0;List<Pokemon> sample=null;
        for(String theme:themes){var team=AuthorRoster.build(theme,new Random(19020261026L));check(team.size()==6,"Six author slots");
            for(var p:team){check(Arrays.stream(p.getEVs().getArray()).allMatch(v->v==252),"All six author EV252");check(Arrays.stream(p.getIVs().getArray()).allMatch(v->v==31),"Author IV31");check(p.getMoveset().size()==4,"Four author moves");members++;}
            for(int slot:new int[]{3,4})check(Set.of(CapSpecies.DEX,493,658,890,898).contains(team.get(slot).getSpecies().getDex()),"Two strong slots preserved");
            for(int slot=0;slot<3;slot++)if(AuthorBossTeams.legendary(team.get(slot).getSpecies())&&team.get(slot).getSpecies().getDex()!=CapSpecies.DEX)check(total(team.get(slot))>=650,"Powerful flexible legendary "+theme);
            if(theme.equals("plains"))check(team.get(0).getSpecies().getDex()==493,"Fixed Arceus");
            if(theme.equals("ocean"))check(team.get(0).getSpecies().getDex()==382&&team.get(0).getForm().isForm("primal"),"Fixed Primal Kyogre");
            if(theme.equals("desert"))check(team.get(0).getSpecies().getDex()==383&&team.get(0).getForm().isForm("primal"),"Fixed Primal Groudon");
            if(theme.equals("swamp"))check(team.get(0).getSpecies().getDex()==890&&team.get(0).getForm().isForm("eternamax"),"Fixed Eternamax");
            if(theme.equals("badlands"))check(team.get(5).getSpecies().getDex()==CapSpecies.DEX,"Fixed Cap");
            if(theme.equals("forest"))sample=team;
        }
        check(sample!=null,"Forest sample exists");
        var candidates=AuthorRoster.class.getDeclaredMethod("candidates",boolean.class);candidates.setAccessible(true);
        for(var s:(List<Species>)candidates.invoke(null,true))check(Arrays.stream(Calamity.STATS).mapToInt(t->s.getDefaultForm().getBattleStats().getStat(t)).sum()>=650,"Full flexible legendary pool650");
        var cover=AuthorRoster.class.getDeclaredMethod("cover",List.class,Random.class);cover.setAccessible(true);Set<String> pairs=new HashSet<>();
        for(int seed=0;seed<32;seed++){var team=new ArrayList<>(sample.subList(0,5));var chosen=(Pokemon)cover.invoke(null,team,new Random(seed));pairs.add(chosen.getSpecies().getName());}
        check(!pairs.isEmpty(),"Legal support choices with identical five core members: "+pairs);
        var npc=new NPCTrainer(player.field_70170_p);npc.getPersistentData().func_74757_a(AuthorBossTeams.MARKER,true);npc.getPersistentData().func_74778_a("AuthorBossTheme","forest");npc.getPersistentData().func_74768_a(AuthorRoster.VERSION,AuthorRoster.CURRENT);npc.getPersistentData().func_74768_a(AuthorRoster.TRAINING,1);npc.getPersistentData().func_74778_a("AuthorArenaWins","sentinel");
        List<String> identities=new ArrayList<>();for(int i=0;i<6;i++){var p=sample.get(i);NpcTraining.maximize(p);npc.getPokemonStorage().set(i,p);identities.add(identity(p));}
        AuthorRoster.upgrade(npc);check(npc.getPersistentData().func_74762_e(AuthorRoster.TRAINING)==2,"Training migration marker2");
        for(int i=0;i<6;i++){var p=npc.getPokemonStorage().get(i);check(identity(p).equals(identities.get(i)),"Migration preserves identity/moves/item");check(Arrays.stream(p.getEVs().getArray()).allMatch(v->v==252),"Migration restores six EV252");}
        AuthorRoster.upgrade(npc);check(npc.getPersistentData().func_74779_i("AuthorArenaWins").equals("sentinel"),"Record survives migration");npc.func_70106_y();
        BossBalance190Probe.log("AUTHORTRIAL190_PASS "+members+" members, fixed/strong slots, legendary pool650, supportChoices="+pairs.size()+", existing team migration no reroll");
        choices(player);
        Update180MomentumCases.run(player);
        Update180R9TeamCases.control(player);
        Stall190Cases.run(player);
        BossBalance190Probe.log("AI190_TACTICS_PASS native priority cleanup, committed control, immunity/PP guards and late lethal reset; all-theme roster validation");
    }
    static void choices(ServerPlayerEntity player)throws Exception{
        var party=StorageProxy.getParty(player);var saved=party.getAll().clone();
        var picker=TacticalAI.class.getDeclaredMethod("pickBestChoice",List.class,List.class);picker.setAccessible(true);
        try{for(boolean author:new boolean[]{false,true}){
            var npc=new NPCTrainer(player.field_70170_p);npc.isGymLeader=true;npc.getPersistentData().func_74778_a(GymCities.TYPE,"psychic");npc.getPersistentData().func_74757_a(AuthorBossTeams.MARKER,author);npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));
            for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,null);npc.getPokemonStorage().set(0,Update180Cases.mon(150,"Ice Beam","Thunderbolt","Recover","Taunt"));
            var foeMon=Update180Cases.mon(143,"Tackle");party.set(0,foeMon);var human=new PlayerParticipant(player,foeMon);var trainer=new TrainerParticipant(npc,1);
            var b=new BattleController(new BattleParticipant[]{trainer},new BattleParticipant[]{human},new BattleRules());b.sendMessages=false;
            try{for(var side:b.participants){side.controlledPokemon.clear();side.controlledPokemon.add(side.allPokemon[0]);for(var w:side.allPokemon){w.bc=b;w.onBattlefield=w==side.allPokemon[0];w.setHealth(w.getMaxHealth());w.pokemon.setHeldItem(net.minecraft.item.ItemStack.field_190927_a);}}
                trainer.setBattleAI(BattleAIMode.ADVANCED.createAI(trainer));var own=trainer.allPokemon[0];var foe=human.allPokemon[0];b.modifyStats();own.choiceLocked=null;
                var a=NpcIntentResponse.raw(own.getBattleAI(),own,new Attack("Ice Beam"),foe);var c=NpcIntentResponse.raw(own.getBattleAI(),own,new Attack("Thunderbolt"),foe);a.tier=c.tier=2;a.weight=100;c.weight=98;var pool=List.of(a,c);Set<String> varied=new HashSet<>();
                for(int turn=1;turn<=128;turn++){b.battleTurn=turn;var chosen=(MoveChoice)picker.invoke(own.getBattleAI(),pool,pool);var again=(MoveChoice)picker.invoke(own.getBattleAI(),pool,pool);check(chosen==again,"No same-turn reroll");varied.add(chosen.attack.getMove().getAttackName());}
                check(varied.size()==2,"Actual injected near-best action variation author="+author);
                c.weight=70;for(int turn=129;turn<160;turn++){b.battleTurn=turn;check(picker.invoke(own.getBattleAI(),pool,pool)==a,"No clearly worse move");}
                c.weight=1000;c.tier=1;check(picker.invoke(own.getBattleAI(),pool,pool)==a,"No loss of tier");c.tier=3;c.weight=180;check(picker.invoke(own.getBattleAI(),pool,pool)==c,"Critical response retains priority");
                c.tier=2;c.weight=98;c.attack.pp=0;check(picker.invoke(own.getBattleAI(),pool,pool)==a,"No exhausted move");c.attack.pp=5;c.result.result=com.pixelmonmod.pixelmon.battles.controller.log.AttackResult.failed;check(picker.invoke(own.getBattleAI(),pool,pool)==a,"No failed move");
                c.result.result=com.pixelmonmod.pixelmon.battles.controller.log.AttackResult.hit;b.simulateMode=true;check(picker.invoke(own.getBattleAI(),pool,pool)==a,"Hypothetical previews retain deterministic best");b.simulateMode=false;
                BossBalance190Probe.log("AI190_VARIATION_PASS author="+author+" actual injected picker, 128 turns, stable repeated planning, score/tier/PP/failed/simulation guards");
            }finally{b.battleEnded=true;NpcTeamKnowledge.end(b);NpcTurnKnowledge.end(b);AuthorForesight.end(b);GymChallenge.end(b);Erosion.end(b);CalamityBattle.end(b);npc.func_70106_y();}
        }}finally{for(int i=0;i<6;i++)party.set(i,saved[i]);}
    }
}
