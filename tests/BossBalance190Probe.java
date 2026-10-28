package qa;

import java.nio.file.*;
import java.util.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.boss.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.spawning.*;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.world.MutableLocation;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/** Native targeted checks in a disposable world, never a production component. */
@Mod("backport_bossbalance190_probe") @Mod.EventBusSubscriber(modid="backport_bossbalance190_probe")
public final class BossBalance190Probe {
    static final String WORLD="backport-boss-balance190";
    static boolean opened,hidden,ran;static volatile boolean done;static int age,warmup;
    static PixelmonEntity drowned,ultimate;
    static boolean enabled(){return WORLD.equals(System.getProperty("backport.manual.world"));}
    @SubscribeEvent public static void gui(net.minecraftforge.client.event.GuiOpenEvent event){if(enabled()&&event.getGui() instanceof com.pixelmonmod.pixelmon.client.gui.battles.BattleScreen)event.setCanceled(true);}
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void log(String message){System.out.println(message);try{Files.writeString(Path.of("bossbalance190-result.txt"),message+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}}
    static Pokemon mon(int dex,int level,String... moves){var p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(level);p.initialize();p.getMoveset().clear();for(String move:moves)p.getMoveset().add(new Attack(move));p.heal();return p;}
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent event){
        if(!enabled()||event.phase!=TickEvent.Phase.END)return;var server=ServerLifecycleHooks.getCurrentServer();
        if(server==null||server.func_184103_al().func_181057_v().isEmpty()||!server.func_240776_a_(net.minecraft.world.storage.FolderName.field_237253_i_).normalize().getFileName().toString().equals(WORLD)||++warmup<100)return;
        if(Files.exists(Path.of("zygarde-only"))){if(!done)try{done=Zygarde190Cases.tick(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("ZYGARDE190_FAIL "+t);done=true;}return;}
        if(Files.exists(Path.of("charm-item-only"))){if(!done)try{Charm190Cases.item(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("CHARM190_ITEM_FAIL "+t);done=true;}return;}
        if(Files.exists(Path.of("charm-only"))){if(!done)try{Charm190Cases.server(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("CHARM190_FAIL "+t);done=true;}return;}
        if(Files.exists(Path.of("whole-matches-only"))){if(!done)try{Conventional190Matches.tick(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("MATCH190_FAIL "+t);done=true;}return;}
        if(Files.exists(Path.of("author-natural-only"))){if(!done)try{done=Author190Cases.natural(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("AUTHOR190_NATURAL_FAIL "+t);done=true;}return;}
        if(done&&Files.exists(Path.of("author-only"))&&!Files.exists(Path.of("author-baseline"))){try{Author190Cases.containers(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("AUTHOR190_CONTAINER_FAIL "+t);}return;}
        if(Files.exists(Path.of("resume-only"))){if(!done)try{var p=server.func_184103_al().func_181057_v().get(0);if(!ran){if(Resume190Cases.live(p)){Resume190Cases.server(p);ran=true;}}else done=Resume190Cases.light(p);}catch(Throwable t){t.printStackTrace();log("RESUME190_SERVER_FAIL "+t);done=true;}return;}
        if(Files.exists(Path.of("r8-only"))){if(!done)try{R8BossTimeCases.run(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("R8_TIMING_FAIL "+t);}finally{ran=true;done=true;}return;}
        if(Files.exists(Path.of("r7-only"))){if(!done)try{R7SpawnCases.run(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("R7_NATIVE_FAIL "+t);}finally{ran=true;done=true;}return;}
        if(Files.exists(Path.of("boss-model-only"))){ran=true;done=true;return;}
        if(ran)return;

        if(Files.exists(Path.of("final-art-only"))){ran=true;done=true;return;}
        if(Files.exists(Path.of("final-reload-only"))){ran=true;try{Final190Cases.reload(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("FINAL190_RELOAD_FAIL "+t);}finally{done=true;}return;}
        if(Files.exists(Path.of("final-only"))){ran=true;try{Final190Cases.run(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("FINAL190_FAIL "+t);}finally{done=true;}return;}
        if(Files.exists(Path.of("daycare-only"))){ran=true;try{DayCare190Cases.run(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("DAYCARE190_FAIL "+t);}finally{done=true;}return;}
        if(Files.exists(Path.of("lightball-only"))){ran=true;try{LightBall190Cases.run(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("LIGHTBALL190_FAIL "+t);}finally{done=true;}return;}
        if(Files.exists(Path.of("den-only"))){ran=true;try{Den190Cases.run(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("DEN190_FAIL "+t);}finally{done=true;}return;}
        if(Files.exists(Path.of("radiant-only"))){ran=true;try{Radiant190Cases.run(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("RADIANT190_FAIL "+t);}finally{done=true;}return;}
        if(Files.exists(Path.of("ohko-only"))){ran=true;try{Ohko190Cases.run(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("OHKO190_FAIL "+t);}finally{done=true;}return;}
        if(Files.exists(Path.of("roster-legacy-only"))){ran=true;try{AuthorRoster190Cases.legacy();}catch(Throwable t){t.printStackTrace();log("ROSTER190_FAIL "+t);}finally{done=true;}return;}
        if(Files.exists(Path.of("roster-only"))){ran=true;try{AuthorRoster190Cases.run(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("ROSTER190_FAIL "+t);}finally{done=true;}return;}
        if(Files.exists(Path.of("author-model-only"))){ran=true;try{Author190Cases.preview(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("AUTHOR190_PREVIEW_FAIL "+t);}finally{done=true;}return;}
        if(Files.exists(Path.of("author-only"))){ran=true;try{Author190Cases.run(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("AUTHOR190_FAIL "+t);}finally{done=true;}return;}
        if(Files.exists(Path.of("status-only"))||Files.exists(Path.of("status-guard-only"))){ran=true;try{var player=server.func_184103_al().func_181057_v().get(0);Status190Cases.run(player);if(Files.exists(Path.of("status-guard-only")))Guard190Cases.run(player);}catch(Throwable t){t.printStackTrace();log("STATUS190_FAIL "+t);}finally{done=true;}return;}
        if(Files.exists(Path.of("guard-only"))){ran=true;try{Guard190Cases.run(server.func_184103_al().func_181057_v().get(0));}catch(Throwable t){t.printStackTrace();log("GUARD190_FAIL "+t);}finally{done=true;}return;}
        ran=true;try{var p=server.func_184103_al().func_181057_v().get(0);training();AuthorTrial190Cases.run(p);spawning(p);curses(p);log("BOSSBALANCE190_PASS normal EV510, author EV1512, author no new Calamity; Drowned Calamity retained; Ultimate no Calamity; bounded AI variation");}
        catch(Throwable t){t.printStackTrace();log("BOSSBALANCE190_FAIL "+t);}finally{done=true;}
    }
    static void training(){
        int cases=0;
        for(int level:new int[]{50,100})for(int dex:new int[]{6,9,65,143,445,700})for(boolean room:new boolean[]{false,true}){
            var p=mon(dex,level,room?"Trick Room":"Tackle","Recover");p.getEVs().fillFromArray(252,252,252,252,252,252);NpcTraining.maximize(p);
            int[] ev=p.getEVs().getArray();check(Arrays.stream(ev).sum()==510&&Arrays.stream(ev).allMatch(v->v>=0&&v<=252),"Normal native EV limit "+dex+"/"+level);
            check(Arrays.stream(p.getIVs().getArray()).allMatch(v->v==31),"Normal IV31 retained");
            var snapshot=PokemonFactory.copy(p);NpcTraining.copy(p,snapshot);check(Arrays.equals(snapshot.getEVs().getArray(),ev),"Battle restoration snapshot preserves510");
            AuthorRoster.train(p);check(Arrays.stream(p.getEVs().getArray()).allMatch(v->v==252),"Author six EV252");cases++;
        }
        log("TRAINING190_PASS "+cases+" native normal/author role and level cases, IVs and restoration snapshots");
    }
    static SpawnInfoPokemon info(String name,Float bossRate){
        var info=new SpawnInfoPokemon();info.set=new SpawnSet();info.setPokemon(com.pixelmonmod.api.pokemon.PokemonSpecificationProxy.create(name));info.minLevel=info.maxLevel=50;info.spawnSpecificShinyRate=0f;info.spawnSpecificPokerusRate=0f;info.spawnSpecificBossRate=bossRate;return info;
    }
    static void spawning(ServerPlayerEntity player){
        int total=0,megaTotal=0;
        for(String id:List.of("common","uncommon","rare","epic","legendary","ultimate","drowned")){
            var tier=BossTierRegistry.getBossTierOrNotBoss(id);check(tier.canRandomlySpawn()&&tier.getWeight()>0,"Runtime naturally enabled tier "+id);total+=tier.getWeight();if(tier.isMega())megaTotal+=tier.getWeight();
        }
        check(total==153&&megaTotal==31&&BossTierRegistry.getBossTierOrNotBoss("drowned").getWeight()==2,"Effective native pool totals153/31, Drowned weight2");
        Map<String,Integer> counts=new TreeMap<>(),megaCounts=new TreeMap<>();
        for(int i=0;i<200000;i++){counts.merge(BossTierRegistry.getRandomSpawnableBossTier().getID(),1,Integer::sum);megaCounts.merge(BossTierRegistry.getRandomSpawnableMegaBossTier().getID(),1,Integer::sum);}
        check(counts.getOrDefault("drowned",0)>2100&&counts.getOrDefault("drowned",0)<3200,"Drowned increased weight2: "+counts);
        check(counts.getOrDefault("ultimate",0)>800&&counts.getOrDefault("ultimate",0)<1900,"Ultimate remains weight1: "+counts);
        check(megaCounts.getOrDefault("ultimate",0)>5000&&!megaCounts.containsKey("drowned"),"Ultimate reachable in mega pool, Drowned excluded by native isMega false");
        var world=player.func_71121_q();var pos=new BlockPos(24,76,40);world.func_175726_f(pos);
        var loc=new SpawnLocation(player,new MutableLocation(world,pos),new HashSet<>(),net.minecraft.block.Blocks.field_150349_c,new HashSet<>(),world.func_226691_t_(pos),true,15,12);
        var spawner=new AbstractSpawner("qa-native-natural"){};
        var normal=info("Psyduck",1f);var mega=info("Charizard",1f);var defaults=info("Psyduck",null);
        Map<String,Integer> built=new TreeMap<>(),megaBuilt=new TreeMap<>();int unforcedBosses=0;
        for(int i=0;i<4000;i++){
            var action=(SpawnActionPokemon)normal.construct(spawner,loc);built.merge(action.bossTier.getID(),1,Integer::sum);
            if(action.bossTier.getID().equals("drowned")&&drowned==null)drowned=action.getOrCreateEntity();
            if(action.bossTier.getID().equals("ultimate")&&ultimate==null)ultimate=action.getOrCreateEntity();
            var megaAction=(SpawnActionPokemon)mega.construct(spawner,loc);megaBuilt.merge(megaAction.bossTier.getID(),1,Integer::sum);
        }
        for(int i=0;i<4000;i++)if(((SpawnActionPokemon)defaults.construct(spawner,loc)).bossTier.isBoss())unforcedBosses++;
        check(unforcedBosses>0&&unforcedBosses<80,"Unforced native default Boss roll remains nonzero, count="+unforcedBosses);
        check(drowned!=null&&ultimate!=null&&DrownedBosses.is(drowned)&&ultimate.getBossTier().getID().equals("ultimate"),"Native spawn action actually constructs both high-tier entities");
        check(!megaBuilt.containsKey("drowned")&&megaBuilt.getOrDefault("ultimate",0)>50,"Normal mega species follows mega pool");
        check(BossTierRegistry.getBossTierOrNotBoss("drowned").getExtraLevels()==60&&BossTierRegistry.getBossTierOrNotBoss("ultimate").getExtraLevels()==50,"High tier levels unchanged");
        var config=com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy.getSpawning().getBossSpawning();
        log("NATURAL190_PASS weighted="+counts+" mega="+megaCounts+" actualNativeConstruction="+built+" megaConstruction="+megaBuilt+" unforcedBosses="+unforcedBosses+"/4000 rate="+config.getBossRate()+" ticks="+config.getBossSpawnTicks()+" chance="+config.getBossSpawnChance());
    }
    static void curses(ServerPlayerEntity player)throws Exception{
        var party=StorageProxy.getParty(player);Pokemon[] saved=party.getAll().clone();
        try{
            duel(player,null,true,false,false);
            duel(player,drowned,false,true,false);
            duel(player,ultimate,false,false,false);
            duel(player,null,true,true,true);
        }finally{for(int i=0;i<6;i++)party.set(i,saved[i]);if(drowned!=null)drowned.func_70106_y();if(ultimate!=null)ultimate.func_70106_y();}
        log("CALAMITY190_PASS author opening/switch no new curse; Drowned curse20 survives end and cure restores; Ultimate no curse; existing curse does not stack");
    }
    static void duel(ServerPlayerEntity player,PixelmonEntity wild,boolean author,boolean expect,boolean existing)throws Exception{
        var own=mon(143,100,"Tackle");var reserve=mon(9,100,"Tackle");var party=StorageProxy.getParty(player);for(int i=0;i<6;i++)party.set(i,i==0?own:i==1?reserve:null);
        int baseline=own.getMaxHealth();var human=new PlayerParticipant(player,own);NPCTrainer npc=null;BattleParticipant opponent;
        if(wild==null){npc=new NPCTrainer(player.field_70170_p);npc.getPokemonStorage().set(0,mon(143,70,"Tackle"));opponent=new TrainerParticipant(npc,1);}else opponent=new WildPixelmonParticipant(wild);
        var battle=new BattleController(new BattleParticipant[]{human},new BattleParticipant[]{opponent},new com.pixelmonmod.pixelmon.battles.api.rules.BattleRules());battle.sendMessages=false;
        var init=BattleController.class.getDeclaredMethod("initBattle");init.setAccessible(true);init.invoke(battle);
        try{
            if(author){npc.getPersistentData().func_74757_a(AuthorBossTeams.MARKER,true);check(GymChallenge.author(battle),"Actual author participant recognized");}
            var active=human.allPokemon[0];
            if(existing)own.getPersistentData().func_74768_a(Calamity.TAG,20);
            new Calamity().start(new com.pixelmonmod.pixelmon.api.events.battles.BattleStartedEvent.Post(battle,new BattleParticipant[]{human},new BattleParticipant[]{opponent}));
            check(Calamity.active(own)==expect&&CalamityBattle.active(active)==expect,"Curse entry author="+author+" existing="+existing+" tier="+(wild==null?"trainer":wild.getBossTier().getID()));
            if(expect){check(own.getPersistentData().func_74762_e(Calamity.TAG)==20&&active.getMaxHealth()==baseline-40,"Curse reduction remains20");}
            else check(active.getMaxHealth()==baseline,"Uncursed author/Ultimate maxHP unchanged");
            var bench=human.allPokemon[1];Calamity.enter(bench);check(Calamity.active(reserve)==(wild==drowned),"Switch-in curse policy");
            CalamityBattle.end(battle);check(Calamity.active(own)==expect,"Permanent source remains after battle end");
            Calamity.cured(own);Calamity.cured(reserve);check(!Calamity.active(own)&&!Calamity.active(reserve),"Native cure entry clears persistent state");
        }finally{CalamityBattle.end(battle);Erosion.end(battle);battle.battleEnded=true;if(npc!=null)npc.func_70106_y();}
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent event){
        if(!enabled()||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;
        if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}
        if(!opened&&mc.field_71462_r instanceof MainMenuScreen){opened=true;try{if(Files.exists(Path.of("author-natural-only")))Author190Cases.prepareNatural(Path.of("saves",WORLD,"level.dat"));mc.func_238191_a_(WORLD);}catch(Throwable t){t.printStackTrace();log("WORLD190_PREPARE_FAIL "+t);done=true;}}
        if(mc.field_71439_g!=null){com.pixelmonmod.pixelmon.client.ServerStorageDisplay.showStarterScreen=false;if(mc.field_71462_r instanceof com.pixelmonmod.pixelmon.client.gui.starter.ChooseStarterScreen)mc.func_147108_a(null);}
        if(Files.exists(Path.of("charm-only"))&&!done)try{Charm190Cases.client();}catch(Throwable t){t.printStackTrace();log("CHARM190_CLIENT_FAIL "+t);done=true;}
        if(Files.exists(Path.of("charm-item-only"))&&!done)try{Charm190Cases.itemClient();}catch(Throwable t){t.printStackTrace();log("CHARM190_ITEM_CLIENT_FAIL "+t);done=true;}
        if(done&&(Files.exists(Path.of("radiant-only"))||Files.exists(Path.of("final-only"))||Files.exists(Path.of("final-art-only")))){if(age==0){if(Files.exists(Path.of("final-only")))try{Trail190Cases.run();}catch(Throwable t){t.printStackTrace();log("TRAIL190_FAIL "+t);}Radiant190Cases.view();}if(age==20)net.minecraft.util.ScreenShotHelper.func_148259_a(mc.field_71412_D,"radiant190-native.png",mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});}
        if(done&&age==0&&Files.exists(Path.of("resume-only")))try{Resume190Cases.client();}catch(Throwable t){t.printStackTrace();log("RESUME190_CLIENT_FAIL "+t);}
        if(done&&age==20&&Files.exists(Path.of("resume-only")))net.minecraft.util.ScreenShotHelper.func_148259_a(mc.field_71412_D,"resume190-daycare.png",mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});
        if(done&&Files.exists(Path.of("boss-model-only"))){if(age==0)Resume190Cases.model();if(age==20)net.minecraft.util.ScreenShotHelper.func_148259_a(mc.field_71412_D,"boss-checker-model.png",mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});}
        if(done){age++;boolean room=(Files.exists(Path.of("author-only"))||Files.exists(Path.of("author-natural-only"))||Files.exists(Path.of("author-model-only")))&&!Files.exists(Path.of("author-baseline"));
            boolean inventories=room&&Files.exists(Path.of("author-only"));String shot=room&&age==(inventories?40:100)?"author-room-190.png":inventories&&age==120?"author-table-diamond-190.png":inventories&&age==180?"author-table-netherite-190.png":null;
            if(shot!=null)net.minecraft.util.ScreenShotHelper.func_148259_a(mc.field_71412_D,shot,mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});
            if(age>(inventories?220:room?120:35))mc.func_71400_g();
        }
    }
}
