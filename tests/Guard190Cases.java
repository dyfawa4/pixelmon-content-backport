package qa;
import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.status.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import net.minecraft.entity.player.ServerPlayerEntity;
/** Compare planned protection with real native effects and real incoming damage. */
public final class Guard190Cases {
    public static void run(ServerPlayerEntity player)throws Exception{
        var field=NpcTurnKnowledge.class.getDeclaredField("CURRENT");field.setAccessible(true);Update180R6Cases.context=(ThreadLocal<Object>)field.get(null);Update180R6Cases.scope=Class.forName("local.pixelmon.backport.NpcTurnKnowledge$Context").getDeclaredConstructor(BattleController.class);Update180R6Cases.scope.setAccessible(true);
        var party=StorageProxy.getParty(player);var saved=party.getAll().clone();JsonArray rows=new JsonArray();int mismatch=0,decisionMismatch=0,failedHit=0,success=0;
        var mon=Update180Cases.mon(748,"Protect","Baneful Bunker","Wide Guard","Scald");NpcTraining.maximize(mon);for(int i=0;i<6;i++)party.set(i,i==0?mon:null);
        var npc=new NPCTrainer(player.field_70170_p);npc.isGymLeader=true;npc.getPersistentData().func_74778_a(GymCities.TYPE,"psychic");npc.getPersistentData().func_74757_a(AuthorBossTeams.MARKER,true);npc.getPersistentData().func_74768_a(AuthorRoster.VERSION,3);npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,i==0?Update180Cases.mon(150,"Psychic","Surf","Thunderbolt","Taunt"):null);
        npc.getPokemonStorage().set(1,Update180Cases.mon(150,"Psychic","Surf","Thunderbolt","Ice Beam"));
        var human=new PlayerParticipant(player,mon);var trainer=new TrainerParticipant(npc,1);var b=new BattleController(new BattleParticipant[]{human},new BattleParticipant[]{trainer},new BattleRules());b.sendMessages=false;var init=BattleController.class.getDeclaredMethod("initBattle");init.setAccessible(true);init.invoke(b);
        try{
            var p=human.controlledPokemon.get(0);var own=trainer.controlledPokemon.get(0);own.getMoveset().clear();for(String m:List.of("Psychic","Surf","Thunderbolt","Taunt"))own.getMoveset().add(new Attack(m));
            var poolMethod=TacticalAI.class.getDeclaredMethod("getWeightedAttackChoices",PixelmonWrapper.class);poolMethod.setAccessible(true);
            int turn=5;
            for(String guard:List.of("Protect","Baneful Bunker","Wide Guard"))for(String hit:List.of("Psychic","Surf"))for(int chain=0;chain<4;chain++)for(int trial=0;trial<12;trial++){
                b.battleTurn=turn++;p.getStatuses().removeIf(s->s instanceof ProtectVariation);p.removeTeamStatus(new WideGuard());p.setHealth(p.getMaxHealth());own.setHealth(own.getMaxHealth());p.protectsInARow=chain;p.takenTurn=own.takenTurn=false;p.choiceLocked=own.choiceLocked=null;own.willEvolve=p.willEvolve=false;own.usingZ=p.usingZ=false;
                Update180R6Cases.action(p,guard,p);Update180R6Cases.action(own,hit,p);b.modifyStats();b.turnList=new ArrayList<>(List.of(p,own));b.turn=0;
                boolean predicted=AuthorForesight.protect(p);int weighted=-1;String weightedResult="absent";
                Update180R6Cases.bind(b);
                try{for(MoveChoice c:(List<MoveChoice>)poolMethod.invoke(own.getBattleAI(),own))if(c.attack!=null&&c.attack.getMove().getAttackName().equals(hit)){weighted=c.result==null?-1:c.result.damage;weightedResult=c.result==null?"none":c.result.result.toString();break;}}
                finally{Update180R6Cases.context.remove();}
                // Clear only preview scratch actions; the real move uses the reserved roll.
                Update180R6Cases.action(p,guard,p);Update180R6Cases.action(own,hit,p);b.turnList=new ArrayList<>(List.of(p,own));b.turn=0;p.useAttack(true);
                boolean actual=p.getStatuses().stream().anyMatch(s->s instanceof ProtectVariation)||p.getStatus(StatusType.WideGuard)!=null;
                int before=p.getHealth();b.turn=1;own.useAttack(true);int loss=before-p.getHealth();
                boolean blocks=actual&&(!guard.equals("Wide Guard")||hit.equals("Surf"));
                if(predicted!=actual)mismatch++;if(blocks)success++;else if(loss>0)failedHit++;
                if((blocks&&weighted>0)||(!blocks&&weighted==0))decisionMismatch++;
                var row=new JsonObject();row.addProperty("guard",guard);row.addProperty("attack",hit);row.addProperty("chain",chain);row.addProperty("predicted_guard",predicted);row.addProperty("actual_guard",actual);row.addProperty("weighted_damage",weighted);row.addProperty("weighted_result",weightedResult);row.addProperty("actual_damage",loss);rows.add(row);
            }
            decisions(b,p,own,trainer.allPokemon[1]);
        }finally{b.battleEnded=true;NpcTeamKnowledge.end(b);NpcTurnKnowledge.end(b);AuthorForesight.end(b);GymChallenge.end(b);Erosion.end(b);CalamityBattle.end(b);npc.func_70106_y();for(int i=0;i<6;i++)party.set(i,saved[i]);}
        var report=new JsonObject();report.addProperty("cases",rows.size());report.addProperty("guard_prediction_mismatches",mismatch);report.addProperty("decision_damage_mismatches",decisionMismatch);report.addProperty("blocked",success);report.addProperty("unblocked_actual_hits",failedHit);report.add("rows",rows);Files.writeString(Path.of("guard190-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
        BossBalance190Probe.log("GUARD190_AUDIT cases="+rows.size()+" predictionMismatch="+mismatch+" decisionMismatch="+decisionMismatch+" blocked="+success+" actualHits="+failedHit);
    }
    static void moves(PixelmonWrapper p,String... names){p.getMoveset().clear();for(var name:names)p.getMoveset().add(new Attack(name));p.choiceLocked=null;}
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static MoveChoice switchTo(PixelmonWrapper own,PixelmonWrapper reserve){var c=new MoveChoice(own,reserve.getPokemonUUID());c.tier=2;c.weight=80;return c;}
    static void decisions(BattleController b,PixelmonWrapper p,PixelmonWrapper own,PixelmonWrapper reserve)throws Exception{
        p.getStatuses().removeIf(s->s instanceof ProtectVariation);p.removeTeamStatus(new WideGuard());p.setHealth(p.getMaxHealth());own.setHealth(own.getMaxHealth());reserve.setHealth(reserve.getMaxHealth());own.getStatuses().clear();own.getBattleStats().clearBattleStats(true);b.battleTurn+=10;
        moves(own,"Psychic","Surf","Thunderbolt","Ice Beam");moves(reserve,"Psychic","Surf","Thunderbolt","Ice Beam");Update180R6Cases.action(own,"Psychic",p);
        Update180R6Cases.action(p,"Wide Guard",p);p.protectsInARow=3;Update180R6Cases.bind(b);
        try{var c=NpcGuardControl.refine(own,switchTo(own,reserve));System.out.println("GUARD190_DEBUG "+c+" damage="+(c.result==null?-1:c.result.damage)+" guard="+NpcSetupControl.submitted(p)+" enabled="+NpcTurnKnowledge.enabled(own)+" sim="+b.simulateMode+" own="+own.getSpecies()+" reserve="+reserve.getSpecies()+" statuses="+own.getStatuses());check(c.isOffensiveMove()&&!c.attack.getMove().getAttackName().equals("Surf")&&c.result.damage>0,"Wide Guard answered with an effective single-target attack");}finally{Update180R6Cases.context.remove();}
        b.battleTurn++;Update180R6Cases.action(p,"Protect",p);p.protectsInARow=0;
        own.getStatuses().add(new Sleep(2));Update180R6Cases.bind(b);try{var original=switchTo(own,reserve);check(NpcGuardControl.refine(own,original)==original,"Do not force a sleeping attacker to stay for a PP trade it cannot make");}finally{Update180R6Cases.context.remove();own.getStatuses().clear();}
        for(var a:own.getMoveset())a.pp=a.getMove().getAttackName().equals("Surf")?15:1;
        Update180R6Cases.bind(b);try{var high=NpcGuardControl.refine(own,switchTo(own,reserve));check(high.isAttack()&&high.attack.getMove().getAttackName().equals("Surf"),"Among safe normal blocked moves, spend the highest remaining PP");}finally{Update180R6Cases.context.remove();}
        moves(own,"Surf","Steel Beam");own.getMoveset().get(0).pp=1;own.getMoveset().get(1).pp=5;
        Update180R6Cases.bind(b);MoveChoice spend;
        try{spend=NpcGuardControl.refine(own,switchTo(own,reserve));check(spend.isOffensiveMove()&&spend.attack.getMove().getAttackName().equals("Surf"),"A higher-PP Steel Beam must not sacrifice health just to trade with protection");check(spend.result.result==com.pixelmonmod.pixelmon.battles.controller.log.AttackResult.failed,"Blocked PP trade does not masquerade as a hit");var important=switchTo(own,reserve);important.tier=3;check(NpcGuardControl.refine(own,important)==important,"Critical switch priority retained");}finally{Update180R6Cases.context.remove();}
        int beforePP=spend.attack.pp;own.setAttack(spend.attack,spend.targets,false);p.takenTurn=own.takenTurn=false;b.turnList=new ArrayList<>(List.of(p,own));b.turn=0;p.useAttack(true);int before=p.getHealth();b.turn=1;own.useAttack(true);check(p.getHealth()==before&&spend.attack.pp==beforePP-1,"Real normal PP consumed once into actual successful guard");
        // A low-damage resisted move versus meaningful super-effective coverage is a productive switch.
        b.battleTurn++;p.getStatuses().removeIf(s->s instanceof ProtectVariation);p.protectsInARow=0;p.setHealth(p.getMaxHealth());Update180R6Cases.action(p,"Protect",p);moves(own,"Surf");moves(reserve,"Thunderbolt");own.getBattleStats().modifyStat(-6,com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.SPECIAL_ATTACK);Update180R6Cases.bind(b);
        try{var clean=NpcGuardControl.class.getDeclaredMethod("clean",PixelmonWrapper.class);clean.setAccessible(true);var future=NpcGuardControl.class.getDeclaredMethod("future",PixelmonWrapper.class,PixelmonWrapper.class);future.setAccessible(true);var target=(PixelmonWrapper)clean.invoke(null,p);boolean clock=NpcTurnKnowledge.future(b,true);try{System.out.println("GUARD190_COVERAGE current="+future.invoke(null,own,target)+" next="+future.invoke(null,reserve,target)+" max="+p.getMaxHealth()+" boosts="+own.getBattleStats().getSumIncreases()+" reserve="+reserve.getHealth()+"/"+reserve.getMaxHealth()+" hazards="+own.getEntryHazards()+" incoming="+NpcTactics.incoming(own,NpcTactics.threats(own)));}finally{NpcTurnKnowledge.future(b,clock);}var original=switchTo(own,reserve);check(NpcGuardControl.refine(own,original)==original,"Productive coverage switch retained");}finally{Update180R6Cases.context.remove();}
        BossBalance190Probe.log("GUARD190_DECISIONS PASS singleTarget/highPP/nativePP/criticalSwitch/coverageSwitch");
    }
}
