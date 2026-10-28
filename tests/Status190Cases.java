package qa;
import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.status.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import net.minecraft.entity.player.ServerPlayerEntity;
/** Current-state preview versus real native action, including clocks and shared random branches. */
public final class Status190Cases {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static final java.lang.reflect.Field confusion;
    static {try{confusion=Confusion.class.getDeclaredField("effectTurns");confusion.setAccessible(true);}catch(Exception e){throw new RuntimeException(e);}}
    static StatusBase status(String name)throws Exception{
        switch(name){case "paralysis":return new Paralysis();case "sleeping":return new Sleep(2);case "waking":return new Sleep(0);case "freeze":case "thaw-move":return new Freeze();case "confused":case "snap-out":var c=new Confusion();confusion.setInt(c,name.equals("snap-out")?1:3);return c;case "flinch":return new Flinch();case "recharge":return new Recharge();default:return null;}
    }
    static int clock(PixelmonWrapper p)throws Exception{for(var s:p.getStatuses()){if(s instanceof Sleep)return ((Sleep)s).effectTurns;if(s instanceof Confusion)return confusion.getInt(s);}return -1;}
    public static void run(ServerPlayerEntity player)throws Exception{
        var field=NpcTurnKnowledge.class.getDeclaredField("CURRENT");field.setAccessible(true);Update180R6Cases.context=(ThreadLocal<Object>)field.get(null);Update180R6Cases.scope=Class.forName("local.pixelmon.backport.NpcTurnKnowledge$Context").getDeclaredConstructor(BattleController.class);Update180R6Cases.scope.setAccessible(true);
        var party=StorageProxy.getParty(player);var saved=party.getAll().clone();var rows=new JsonArray();int mismatch=0;
        try{for(boolean author:new boolean[]{false,true}){
            var mon=Update180Cases.mon(748,"Scald","Protect","Sleep Talk","Snore");NpcTraining.maximize(mon);for(int i=0;i<6;i++)party.set(i,i==0?mon:null);
            var npc=new NPCTrainer(player.field_70170_p);npc.isGymLeader=true;npc.getPersistentData().func_74778_a(GymCities.TYPE,"psychic");npc.getPersistentData().func_74757_a(AuthorBossTeams.MARKER,author);npc.getPersistentData().func_74768_a(AuthorRoster.VERSION,3);npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,i==0?Update180Cases.mon(150,"Psychic","Surf","Thunderbolt","Protect"):null);
            var human=new PlayerParticipant(player,mon);var trainer=new TrainerParticipant(npc,1);var b=new BattleController(new BattleParticipant[]{human},new BattleParticipant[]{trainer},new BattleRules());b.sendMessages=false;var init=BattleController.class.getDeclaredMethod("initBattle");init.setAccessible(true);init.invoke(b);
            try{var p=human.controlledPokemon.get(0);var own=trainer.controlledPokemon.get(0);b.battleTurn=5;
                for(boolean actorNpc:new boolean[]{false,true})for(String name:List.of("healthy","paralysis","sleeping","waking","freeze","thaw-move","confused","snap-out","flinch","recharge"))for(int trial=0;trial<32;trial++){
                    b.battleTurn++;var actor=actorNpc?own:p;var target=actorNpc?p:own;
                    for(var w:List.of(p,own)){w.getStatuses().clear();w.setHealth(w.getMaxHealth());w.canAttack=true;w.isSwitching=false;w.getBattleStats().clearBattleStats(true);w.willEvolve=false;w.usingZ=false;w.getMoveset().clear();w.getMoveset().add(new Attack("Surf"));}
                    var s=status(name);if(s!=null)actor.getStatuses().add(s);String move=name.equals("thaw-move")?"Scald":"Surf";Update180R6Cases.action(actor,move,target);Update180R6Cases.action(target,"Protect",target);b.modifyStats();b.turnList=new ArrayList<>(List.of(actor,target));b.turn=0;
                    int beforeClock=clock(actor),beforeHP=actor.getHealth();boolean expected;
                    Update180R6Cases.bind(b);try{expected=NpcActionAvailability.canAct(actor,actor.attack);for(int i=0;i<4;i++)check(expected==NpcActionAvailability.canAct(actor,actor.attack),"Repeated preview stable");check(clock(actor)==beforeClock&&actor.getHealth()==beforeHP,"Preview cannot advance clocks or inflict self damage");check(AuthorForesight.succeeds(actor,target,actor.attack)==expected,"Shared success forecast includes status availability");if(!actorNpc){check(!NpcTurnKnowledge.moves(actor).isEmpty(),"Committed damaging action is retained even if unable to execute, for Sucker Punch semantics");check((NpcTactics.incoming(own,NpcTactics.threats(own))>0)==expected,"Enemy damage forecast excludes blocked action");}}finally{Update180R6Cases.context.remove();}
                    int hp=target.getHealth();actor.takeTurn();boolean actual=target.getHealth()<hp;if(expected!=actual)mismatch++;
                    var r=new JsonObject();r.addProperty("author",author);r.addProperty("actor",actorNpc?"npc":"player");r.addProperty("status",name);r.addProperty("preview_can_act",expected);r.addProperty("actual_hit",actual);r.addProperty("own_hp_loss",beforeHP-actor.getHealth());r.addProperty("clock_before",beforeClock);r.addProperty("clock_after",clock(actor));rows.add(r);
                }
                var pool=com.pixelmonmod.pixelmon.battles.controller.ai.TacticalAI.class.getDeclaredMethod("getWeightedAttackChoices",PixelmonWrapper.class);pool.setAccessible(true);
                for(String state:List.of("paralysis","sleeping","waking","freeze","confused"))for(String guard:List.of("Protect","Baneful Bunker"))for(int trial=0;trial<16;trial++){
                    b.battleTurn++;for(var w:List.of(p,own)){w.getStatuses().clear();w.setHealth(w.getMaxHealth());w.canAttack=true;w.getMoveset().clear();w.getMoveset().add(new Attack("Surf"));w.choiceLocked=null;w.takenTurn=false;}
                    p.getStatuses().add(status(state));p.protectsInARow=0;Update180R6Cases.action(p,guard,p);Update180R6Cases.action(own,"Surf",p);b.modifyStats();b.turnList=new ArrayList<>(List.of(p,own));b.turn=0;
                    int damage=-1;Update180R6Cases.bind(b);try{for(var c:(List<com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice>)pool.invoke(own.getBattleAI(),own))if(c.attack!=null&&c.attack.getMove().getAttackName().equals("Surf")){damage=c.result==null?-1:c.result.damage;break;}}finally{Update180R6Cases.context.remove();}
                    p.takeTurn();int hp=p.getHealth();b.turn=1;own.takeTurn();boolean hit=p.getHealth()<hp;check(damage>=0,"Native candidate exists for status+guard");if((damage>0)!=hit)mismatch++;
                    var row=new JsonObject();row.addProperty("author",author);row.addProperty("actor","guard-integration");row.addProperty("status",state);row.addProperty("guard",guard);row.addProperty("preview_can_act",damage>0);row.addProperty("actual_hit",hit);row.addProperty("weighted_damage",damage);rows.add(row);
                }
                b.battleTurn++;p.getStatuses().clear();own.getStatuses().clear();p.setHealth(p.getMaxHealth());own.setHealth(own.getMaxHealth());p.canAttack=false;Update180R6Cases.action(p,"Surf",own);b.turn=0;b.turnList=new ArrayList<>(List.of(p,own));Update180R6Cases.bind(b);try{check(NpcActionAvailability.canAct(p,p.attack),"Cached prior failure does not override native next-attempt recalculation");}finally{Update180R6Cases.context.remove();}int prior=own.getHealth();p.takeTurn();check(own.getHealth()<prior&&p.canAttack,"Native actual next attempt recomputes cached canAttack");
                // A real status cure between planning and execution must invalidate a blocked forecast.
                b.battleTurn++;p.getStatuses().clear();p.getStatuses().add(new Sleep(2));p.canAttack=true;Update180R6Cases.action(p,"Surf",own);Update180R6Cases.bind(b);
                try{check(!NpcActionAvailability.canAct(p,p.attack),"Initially sleeping");p.removeStatus(p.getStatus(StatusType.Sleep));check(NpcActionAvailability.canAct(p,p.attack)&&!NpcTurnKnowledge.moves(p).isEmpty(),"Live cure immediately changes current action availability without stale cache");}finally{Update180R6Cases.context.remove();}
                // Extra attempted actions in one global turn need fresh status rolls even after a failed attempt.
                b.battleTurn++;int hits=0;
                for(int attempt=0;attempt<64;attempt++){
                    p.getStatuses().clear();p.getStatuses().add(new Paralysis());p.canAttack=true;p.setHealth(p.getMaxHealth());own.getStatuses().clear();own.setHealth(own.getMaxHealth());Update180R6Cases.action(p,"Surf",own);b.turn=0;b.turnList=new ArrayList<>(List.of(p,own));boolean expected;Update180R6Cases.bind(b);try{expected=NpcActionAvailability.canAct(p,p.attack);}finally{Update180R6Cases.context.remove();}int hp=own.getHealth();p.takeTurn();boolean actual=own.getHealth()<hp;if(actual)hits++;if(expected!=actual)mismatch++;var row=new JsonObject();row.addProperty("author",author);row.addProperty("actor","same-turn-extra-attempt");row.addProperty("status","paralysis");row.addProperty("preview_can_act",expected);row.addProperty("actual_hit",actual);rows.add(row);
                }check(hits>0&&hits<64,"Same-turn attempts have both status branches, not one repeated stale result");
                boolean checkedPunch=false;
                for(int trial=0;trial<100&&!checkedPunch;trial++){
                    b.battleTurn++;p.getStatuses().clear();p.getStatuses().add(new Paralysis());own.getStatuses().clear();p.canAttack=own.canAttack=true;p.setHealth(p.getMaxHealth());own.setHealth(own.getMaxHealth());Update180R6Cases.action(p,"Surf",own);Update180R6Cases.action(own,"Sucker Punch",p);b.turn=0;b.turnList=new ArrayList<>(List.of(own,p));b.modifyStats();Update180R6Cases.bind(b);
                    try{if(NpcActionAvailability.canAct(p,p.attack))continue;var c=NpcIntentResponse.raw(own.getBattleAI(),own,own.attack,p);NpcIntentResponse.conditional(own.getBattleAI(),c);check(c.result.damage>0,"Sucker Punch still hits a submitted damaging action even when future status will prevent it");checkedPunch=true;}finally{Update180R6Cases.context.remove();}
                    int hp=p.getHealth();own.takeTurn();check(p.getHealth()<hp,"Actual Sucker Punch hits before the blocked damaging action");b.turn=1;hp=own.getHealth();p.takeTurn();check(own.getHealth()==hp,"Target's shared full-paralysis result still blocks its later action");
                }check(checkedPunch,"Full-paralysis conditional-move boundary covered");
                // Native exceptions for sleep moves, including a waking turn where the move itself fails.
                for(String move:List.of("Snore","Sleep Talk"))for(int turns:new int[]{0,2}){
                    p.getStatuses().clear();p.getStatuses().add(new Sleep(turns));p.canAttack=true;p.setHealth(p.getMaxHealth());own.getStatuses().clear();own.setHealth(own.getMaxHealth());p.getMoveset().clear();p.getMoveset().add(new Attack("Surf"));b.battleTurn++;Update180R6Cases.action(p,move,own);b.turn=0;b.turnList=new ArrayList<>(List.of(p,own));boolean expected;Update180R6Cases.bind(b);try{expected=NpcActionAvailability.canAct(p,p.attack);check(expected==(turns>0),"Sleep move usable while asleep, fails when waking");}finally{Update180R6Cases.context.remove();}int hp=own.getHealth();p.takeTurn();boolean actual=own.getHealth()<hp;if(expected!=actual)mismatch++;var row=new JsonObject();row.addProperty("author",author);row.addProperty("actor","sleep-move");row.addProperty("status",move+"/"+turns);row.addProperty("preview_can_act",expected);row.addProperty("actual_hit",actual);rows.add(row);
                }
            }finally{b.battleEnded=true;NpcTeamKnowledge.end(b);NpcTurnKnowledge.end(b);AuthorForesight.end(b);GymChallenge.end(b);Erosion.end(b);CalamityBattle.end(b);npc.func_70106_y();}
        }}finally{for(int i=0;i<6;i++)party.set(i,saved[i]);}
        var report=new JsonObject();report.addProperty("cases",rows.size());report.addProperty("mismatches",mismatch);report.add("rows",rows);Files.writeString(Path.of("status190-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));BossBalance190Probe.log("STATUS190_AUDIT cases="+rows.size()+" mismatches="+mismatch);check(mismatch==0,"Status prediction differs from actual native execution");
    }
}
