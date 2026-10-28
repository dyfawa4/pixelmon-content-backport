package qa;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.status.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import net.minecraft.entity.player.ServerPlayerEntity;
import local.pixelmon.backport.*;

/** Native OHKO accuracy, type, ability, level and space probes; no forced random results. */
public final class Ohko190Cases {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(ServerPlayerEntity player)throws Exception{
        var field=NpcTurnKnowledge.class.getDeclaredField("CURRENT");field.setAccessible(true);Update180R6Cases.context=(ThreadLocal<Object>)field.get(null);Update180R6Cases.scope=Class.forName("local.pixelmon.backport.NpcTurnKnowledge$Context").getDeclaredConstructor(BattleController.class);Update180R6Cases.scope.setAccessible(true);
        var party=StorageProxy.getParty(player);var saved=party.getAll().clone();JsonArray rows=new JsonArray();int mismatches=0;
        try{for(boolean author:new boolean[]{false,true})for(boolean npcActor:new boolean[]{false,true})for(String scenario:List.of("equal","higher-target","lower-target","ice","flying","ghost","sturdy","mold-breaker","no-guard","stages","protect","trick-room")){
            int targetDex=scenario.equals("ice")?131:scenario.equals("flying")?6:scenario.equals("ghost")?94:scenario.equals("sturdy")||scenario.equals("mold-breaker")?76:143;
            var actorMon=Update180Cases.mon(131,"Fissure","Sheer Cold","Horn Drill","Guillotine");var targetMon=Update180Cases.mon(targetDex,"Tackle");NpcTraining.maximize(actorMon);NpcTraining.maximize(targetMon);actorMon.setHeldItem(net.minecraft.item.ItemStack.field_190927_a);targetMon.setHeldItem(net.minecraft.item.ItemStack.field_190927_a);
            if(scenario.equals("sturdy")||scenario.equals("mold-breaker"))targetMon.setAbility(new com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Sturdy());
            if(scenario.equals("mold-breaker"))actorMon.setAbility(new com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.MoldBreaker());
            if(scenario.equals("no-guard"))actorMon.setAbility(new com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.NoGuard());
            for(int i=0;i<6;i++)party.set(i,i==0?(npcActor?targetMon:actorMon):null);
            var npc=new NPCTrainer(player.field_70170_p);npc.isGymLeader=true;npc.getPersistentData().func_74778_a(GymCities.TYPE,"psychic");npc.getPersistentData().func_74757_a(AuthorBossTeams.MARKER,author);npc.getPersistentData().func_74768_a(AuthorRoster.VERSION,AuthorRoster.CURRENT);npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,i==0?(npcActor?actorMon:targetMon):null);
            var human=new PlayerParticipant(player,npcActor?targetMon:actorMon);var trainer=new TrainerParticipant(npc,1);var b=new BattleController(new BattleParticipant[]{human},new BattleParticipant[]{trainer},new BattleRules());b.sendMessages=false;
            var init=BattleController.class.getDeclaredMethod("initBattle");init.setAccessible(true);
            // Keep this exact semantic fixture, instead of expanding an ordinary
            // leader to its real six-member roster before checking target types.
            npc.getPersistentData().func_74757_a("BackportNpcDisable",true);init.invoke(b);npc.getPersistentData().func_74757_a("BackportNpcDisable",false);
            trainer.setBattleAI(com.pixelmonmod.pixelmon.api.battles.BattleAIMode.ADVANCED.createAI(trainer));
            try{
                var actor=npcActor?trainer.controlledPokemon.get(0):human.controlledPokemon.get(0);var target=npcActor?human.controlledPokemon.get(0):trainer.controlledPokemon.get(0);
                check(actor.pokemon.getUUID().equals(actorMon.getUUID())&&target.pokemon.getUUID().equals(targetMon.getUUID()),"Exact targeted fixture members retained");
                actor.setTempLevel(scenario.equals("higher-target")?99:100);target.setTempLevel(scenario.equals("lower-target")?90:100);
                actor.pokemon.setHeldItem(net.minecraft.item.ItemStack.field_190927_a);target.pokemon.setHeldItem(net.minecraft.item.ItemStack.field_190927_a);actor.tempAbility=actorMon.getAbility();target.tempAbility=targetMon.getAbility();
                if(scenario.equals("sturdy")||scenario.equals("mold-breaker"))target.tempAbility=new com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Sturdy();
                if(scenario.equals("mold-breaker"))actor.tempAbility=new com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.MoldBreaker();
                if(scenario.equals("no-guard"))actor.tempAbility=new com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.NoGuard();
                if(scenario.equals("trick-room"))actor.addGlobalStatus(new TrickRoom());
                for(String move:List.of("Fissure","Sheer Cold","Horn Drill","Guillotine"))for(int trial=0;trial<24;trial++){
                    b.battleTurn++;for(var w:List.of(actor,target)){w.getStatuses().clear();w.setHealth(w.getMaxHealth());w.canAttack=true;w.isSwitching=false;w.getBattleStats().clearBattleStats(true);w.willEvolve=false;w.usingZ=false;w.getMoveset().clear();}
                    actor.getMoveset().add(new Attack(move));target.getMoveset().add(new Attack("Tackle"));Update180R6Cases.action(actor,move,target);Update180R6Cases.action(target,"Tackle",actor);b.modifyStats();b.turnList=new ArrayList<>(List.of(actor,target));b.turn=0;
                    if(scenario.equals("stages")){actor.getBattleStats().modifyStat(6,com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.ACCURACY);target.getBattleStats().modifyStat(6,com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.EVASION);}
                    if(scenario.equals("protect"))target.getStatuses().add(new Protect());
                    int before=target.getHealth();boolean predicted;int previewDamage;Update180R6Cases.bind(b);
                    try{var result=NpcIntentResponse.raw(trainer.controlledPokemon.get(0).getBattleAI(),actor,actor.attack,target);previewDamage=result.result==null?0:result.result.damage;predicted=previewDamage>0&&AuthorForesight.succeeds(actor,target,actor.attack);check(target.getHealth()==before,"Preview cannot damage real target");}
                    finally{Update180R6Cases.context.remove();}
                    actor.takeTurn();int loss=before-target.getHealth();boolean hit=loss>0;if(predicted!=hit)mismatches++;
                    JsonObject row=new JsonObject();row.addProperty("author",author);row.addProperty("actor",npcActor?"npc":"player");row.addProperty("scenario",scenario);row.addProperty("move",move);row.addProperty("preview_hit",predicted);row.addProperty("preview_damage",previewDamage);row.addProperty("actual_hit",hit);row.addProperty("actual_damage",loss);row.addProperty("actual_accuracy",actor.attack.moveResult==null?-999:actor.attack.moveResult.accuracy);row.addProperty("actor_level",actor.getPokemonLevelNum());row.addProperty("target_level",target.getPokemonLevelNum());rows.add(row);
                }
            }finally{b.battleEnded=true;NpcTeamKnowledge.end(b);NpcTurnKnowledge.end(b);AuthorForesight.end(b);GymChallenge.end(b);Erosion.end(b);CalamityBattle.end(b);npc.func_70106_y();}
        }}finally{for(int i=0;i<6;i++)party.set(i,saved[i]);}
        var report=new JsonObject();report.addProperty("cases",rows.size());report.addProperty("mismatches",mismatches);report.add("rows",rows);Files.writeString(Path.of("ohko190-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));BossBalance190Probe.log("OHKO190_AUDIT cases="+rows.size()+" mismatches="+mismatches);if(!Files.exists(Path.of("ohko-baseline")))check(mismatches==0,"OHKO prediction differs from actual native execution");
    }
}
