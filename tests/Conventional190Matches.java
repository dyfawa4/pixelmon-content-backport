package qa;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.*;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import net.minecraft.entity.player.ServerPlayerEntity;
/** Private whole matches: accelerate visual/tick waits, never damage, PP or combat state. */
public final class Conventional190Matches {
    static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    static final JsonArray results=new JsonArray();
    static JsonArray teams;static int index,updates,stalled,lastTurn;static boolean running;static FastBattle battle;static BotPlayer bot;static TrainerParticipant defender;static NPCTrainer npc;static Pokemon[] saved;
    static JsonObject result;static JsonArray turns;static long start,lastProgress;static int readyTicks;static List<Pokemon> pendingMembers;static JsonObject pendingSource;static int pendingMode;
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    static final class FastBattle extends BattleController {
        FastBattle(BattleParticipant a,BattleParticipant b){super(new BattleParticipant[]{a},new BattleParticipant[]{b},new BattleRules());}
        void advance(){
            // The GUI normally acknowledges message/animation waits. This harness
            // has no interactive battle screen; wrapper.wait still protects missing input.
            for(var side:participants)side.wait=false;
            battleTicks=21;currentAnimations.clear();update();
        }
    }
    static final class BotPlayer extends PlayerParticipant {
        BotPlayer(ServerPlayerEntity p,Pokemon first){super(p,first);}
        @Override public boolean canGainXP(){return false;}
        @Override public void selectAction(){wait=false;for(var own:controlledPokemon)if(own.isAlive()&&!own.skipsTurn()){var c=getMove(own);if(c!=null&&c.isAttack())own.setAttack(c.attack,c.targets,false);else own.chooseMove(c);}wait=false;}
        @Override public MoveChoice getMove(PixelmonWrapper own){
            // Native AdvancedAI was written for NPC participants; its hypothetical
            // PlayerParticipant switches leave GUI wait/field flags behind. Mirror
            // the production trainer preview restoration in this player-input adapter.
            Map<BattleParticipant,Boolean> waits=new IdentityHashMap<>();Map<PixelmonWrapper,boolean[]> flags=new IdentityHashMap<>();
            for(var side:bc.participants){waits.put(side,side.wait);for(var p:side.allPokemon)flags.put(p,new boolean[]{p.onBattlefield,p.switchedThisTurn,p.takenTurn,p.hasBeenInBattle,p.wait,p.canAttack});}
            try{var scripted=Match190Scripts.choose(own);return scripted==null?getBattleAI().getNextMove(own):scripted;}finally{waits.forEach((p,v)->p.wait=v);flags.forEach((p,v)->{p.onBattlefield=v[0];p.switchedThisTurn=v[1];p.takenTurn=v[2];p.hasBeenInBattle=v[3];p.wait=v[4];p.canAttack=v[5];});}
        }
        @Override public void getNextPokemon(int slot){
            if(slot>=controlledPokemon.size())return;var old=controlledPokemon.get(slot);
            var next=Arrays.stream(allPokemon).filter(p->p.isAlive()&&!p.onBattlefield&&!switchingIn.contains(p.getPokemonUUID())).max(Comparator.comparingDouble(PixelmonWrapper::getHealthPercent)).orElse(null);
            if(next!=null)bc.switchPokemon(old.getPokemonUUID(),next.getPokemonUUID(),true);wait=false;old.wait=false;
        }
        void answerPivot(){
            for(int i=0;i<controlledPokemon.size();i++){var own=controlledPokemon.get(i);
                if(own.wait&&own.nextSwitchIsMove&&own.newPokemonUUID==null&&own.attack!=null)getNextPokemon(i);
            }
        }
    }
    static JsonObject row(Pokemon p){
        var r=new JsonObject();r.addProperty("species",p.getSpecies().getName());r.addProperty("dex",p.getSpecies().getDex());r.addProperty("form",p.getForm().getName());r.addProperty("level",p.getPokemonLevel());r.addProperty("nature",p.getNature().name());r.add("ivs",GSON.toJsonTree(p.getIVs().getArray()));r.add("evs",GSON.toJsonTree(p.getEVs().getArray()));r.addProperty("ability",p.getAbility().getClass().getSimpleName());r.addProperty("item",p.getHeldItem().func_77973_b().getRegistryName().toString());JsonArray moves=new JsonArray();p.getMoveset().forEach(a->moves.add(a.getMove().getAttackName()));r.add("moves",moves);return r;
    }
    static List<Pokemon> team(JsonObject source){
        List<Pokemon> list=new ArrayList<>();for(var member:source.getAsJsonArray("members")){var m=member.getAsJsonObject();var p=Update180Cases.mon(m.get("dex").getAsInt());if(m.has("form"))p.setForm(m.get("form").getAsString());p.getMoveset().clear();
            for(var move:m.getAsJsonArray("moves")){var a=new Attack(move.getAsString());check(p.getForm().getMoves().getAllMoves().stream().anyMatch(v->v.getAttackName().equals(a.getMove().getAttackName())),"Challenger learnset "+p.getSpecies().getName()+"/"+p.getForm().getName()+" "+move);p.getMoveset().add(a);}
            if(m.has("item"))p.setHeldItem(Update180Cases.stack("pixelmon:"+m.get("item").getAsString()));NpcTraining.maximize(p);
            if(m.has("ability")){String wanted=m.get("ability").getAsString();var ability=Arrays.stream(p.getForm().getAbilities().getAll()).filter(Objects::nonNull).filter(a->a.getClass().getSimpleName().equals(wanted)).findFirst().orElseThrow(()->new AssertionError("Illegal challenger ability "+wanted));p.setAbility(ability);}
            check(Arrays.stream(p.getEVs().getArray()).sum()==510&&Arrays.stream(p.getEVs().getArray()).allMatch(v->v<=252),"Legal challenger510");check(Arrays.stream(p.getIVs().getArray()).allMatch(v->v<=31),"Legal challenger IV31");p.heal();list.add(p);
        }check(list.size()==6,"Six challenger members");return list;
    }
    public static void tick(ServerPlayerEntity player)throws Exception{
        if(teams==null){teams=new JsonParser().parse(Files.readString(Path.of("conventional190-teams.json"))).getAsJsonArray();saved=StorageProxy.getParty(player).getAll().clone();if(Files.exists(Path.of("only-case")))index=Integer.parseInt(Files.readString(Path.of("only-case")).trim());}
        if(readyTicks>0){if(--readyTicks==0)startBattle(player);return;}
        if(!running){if(index>=teams.size()*3||Files.exists(Path.of("only-case"))&&results.size()>0){for(int i=0;i<6;i++)StorageProxy.getParty(player).set(i,saved[i]);Files.writeString(Path.of("conventional190-result.txt"),"MATCH190_SUITE_FINISHED matches="+results.size()+"\n");BossBalance190Probe.done=true;return;}begin(player);return;}
        long until=System.nanoTime()+35_000_000L;int batch=0;
        do{bot.answerPivot();battle.advance();Match190Scripts.observe();updates++;if(battle.battleTurn!=lastTurn){lastTurn=battle.battleTurn;stalled=0;lastProgress=System.currentTimeMillis();record();}else stalled++;
            if(stalled==500){for(var side:battle.participants)for(var own:side.controlledPokemon)System.out.println("MATCH190_WAIT "+side.getClass().getSimpleName()+" partyWait="+side.wait+" wrapperWait="+own.wait+" switch="+own.isSwitching+" active="+own.onBattlefield+" action="+own.attack+" new="+own.newPokemonUUID+" stage="+battle.getStage()+" sim="+battle.simulateMode);}
            if(battle.battleEnded||battle.battleTurn>=180||updates>=40000||System.currentTimeMillis()-lastProgress>45000){complete();break;}
            if(battle.isFrozen())break;
        }while(++batch<8&&System.nanoTime()<until);
    }
    static void begin(ServerPlayerEntity player)throws Exception{
        int mode=index%3;JsonObject source=teams.get(index/3).getAsJsonObject();var members=team(source);var party=StorageProxy.getParty(player);for(int i=0;i<6;i++)party.set(i,members.get(i));
        npc=new NPCTrainer(player.field_70170_p);npc.func_70107_b(player.func_226277_ct_()+5,player.func_226278_cu_(),player.func_226281_cx_());npc.isGymLeader=true;npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,null);
        if(mode<2){String theme=mode==0?"plains":"ocean";npc.getPersistentData().func_74757_a(AuthorBossTeams.MARKER,true);npc.getPersistentData().func_74778_a("AuthorBossTheme",theme);npc.getPersistentData().func_74768_a(AuthorRoster.VERSION,AuthorRoster.CURRENT);var roster=AuthorRoster.build(theme,new Random(190000+index));for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,roster.get(i));}
        else{npc.getPersistentData().func_74778_a(GymCities.TYPE,"steel");npc.getPokemonStorage().set(0,Update180Cases.mon(376,"Meteor Mash","Zen Headbutt","Earthquake","Bullet Punch"));}
        player.func_71121_q().func_217376_c(npc);pendingMembers=members;pendingSource=source;pendingMode=mode;readyTicks=10;
    }
    static void startBattle(ServerPlayerEntity player)throws Exception{
        var source=pendingSource;int mode=pendingMode;var members=pendingMembers;
        bot=new BotPlayer(player,members.get(0));defender=new TrainerParticipant(npc,1);battle=new FastBattle(bot,defender);battle.sendMessages=false;BattleRegistry.registerBattle(battle);var init=BattleController.class.getDeclaredMethod("initBattle");init.setAccessible(true);init.invoke(battle);bot.setBattleAI(new AdvancedAI(bot));battle.endPause();
        result=new JsonObject();result.addProperty("archetype",source.get("name").getAsString());result.addProperty("opponent",mode==0?"author-plains":mode==1?"author-ocean":"ordinary-steel");JsonArray challengers=new JsonArray(),opponents=new JsonArray();for(var p:bot.allPokemon)challengers.add(row(p.pokemon));for(var p:defender.allPokemon)opponents.add(row(p.pokemon));result.add("challengers",challengers);result.add("defenders",opponents);turns=new JsonArray();result.add("turns",turns);updates=stalled=0;lastTurn=-999;start=lastProgress=System.currentTimeMillis();running=true;Match190Scripts.start();System.out.println("MATCH190_START "+index+" "+source.get("name")+" mode="+mode);
    }
    static void record(){
        var turn=new JsonObject();turn.addProperty("turn",battle.battleTurn);JsonArray rows=new JsonArray();
        for(var side:battle.participants)for(var p:side.allPokemon){JsonObject r=new JsonObject();r.addProperty("side",side==bot?"challenger":"defender");r.addProperty("pokemon",p.getSpecies().getName()+"/"+p.pokemon.getForm().getName());r.addProperty("alive",p.isAlive());r.addProperty("active",p.onBattlefield);r.addProperty("hp",p.getHealth());r.addProperty("max_hp",p.getMaxHealth());r.addProperty("last_move",p.lastAttack==null?"":p.lastAttack.getMove().getAttackName());r.addProperty("chosen_move",p.attack==null?"":p.attack.getMove().getAttackName());r.addProperty("switch_to",p.newPokemonUUID==null?"":p.newPokemonUUID.toString());r.add("pp",GSON.toJsonTree(p.getMoveset().stream().mapToInt(a->a.pp).toArray()));r.addProperty("statuses",p.getStatuses().toString());rows.add(r);}turn.add("members",rows);turns.add(turn);
    }
    static void complete()throws Exception{
        int human=(int)Arrays.stream(bot.allPokemon).filter(PixelmonWrapper::isAlive).count(),ai=(int)Arrays.stream(defender.allPokemon).filter(PixelmonWrapper::isAlive).count();
        String outcome=battle.battleEnded?(bot.isDefeated?"defender-win":defender.isDefeated?"challenger-win":human==0?"defender-win":ai==0?"challenger-win":"ended-without-defeat"):battle.battleTurn>=180?"turn-limit":"stalled";
        record();result.addProperty("outcome",outcome);result.addProperty("total_turns",battle.battleTurn);result.addProperty("challenger_remaining",human);result.addProperty("defender_remaining",ai);result.addProperty("engine_updates",updates);result.addProperty("elapsed_ms",System.currentTimeMillis()-start);results.add(result);Files.writeString(Path.of("conventional190-results.json"),GSON.toJson(results));System.out.println("MATCH190_RESULT "+index+" "+result.get("archetype")+" "+result.get("opponent")+" "+outcome+" turns="+battle.battleTurn+" alive="+human+"/"+ai+" stage="+battle.getStage());
        if(!battle.battleEnded)battle.endBattle(com.pixelmonmod.pixelmon.api.battles.BattleEndCause.FORCE);NpcTeamKnowledge.end(battle);NpcTurnKnowledge.end(battle);AuthorForesight.end(battle);GymChallenge.end(battle);Erosion.end(battle);CalamityBattle.end(battle);BattleRegistry.deRegisterBattle(battle);npc.func_70106_y();running=false;index++;
    }
}
