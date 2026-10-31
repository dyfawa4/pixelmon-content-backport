package qa;
import java.io.*;import java.nio.file.*;import java.util.*;import com.google.gson.*;
import net.minecraftforge.fml.common.Mod;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.event.TickEvent;import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraft.client.Minecraft;import net.minecraft.nbt.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;import com.pixelmonmod.pixelmon.battles.attacks.Attack;import com.pixelmonmod.pixelmon.battles.controller.*;import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import local.pixelmon.backport.*;
@Mod("backport_nature110_probe") @Mod.EventBusSubscriber(modid="backport_nature110_probe")
public class Nature110Probe {
 static boolean opened,hidden,done;static int ticks,checks,templates,trainers,members,leaders,grunts;static List<String> paths;static int cursor;static final Set<String> themes=new TreeSet<>();static final JsonArray rows=new JsonArray();
 static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
 static Pokemon mon(int dex,String...moves){var p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(70);p.initialize();p.getMoveset().clear();for(var m:moves)p.getMoveset().add(new Attack(m));return p;}
 static String stable(Pokemon p){return p.getUUID()+"/"+p.getForm().getName()+"/"+p.getPokemonLevel()+"/"+p.getMoveset()+"/"+p.getHeldItem()+Arrays.toString(p.getEVs().getArray())+Arrays.toString(p.getIVs().getArray());}
 static void inspect(Pokemon p,String label){
  var nature=p.getNature();check(nature==p.getBaseNature()&&p.getMintNature()==null,"Visible/effective nature mismatch "+label);check(nature.getIncreasedStat()!=nature.getDecreasedStat(),"Neutral nature "+label);
  check(nature.getDecreasedStat()!=BattleStatsType.SPEED||p.getMoveset().stream().anyMatch(a->a.getMove().getAttackName().equals("Trick Room")),"Speed lost without own Trick Room "+label);
  String id=stable(p);var restored=PokemonFactory.create(p.writeToNBT(new CompoundNBT()));check(restored.getNature()==nature&&restored.getBaseNature()==nature&&stable(restored).equals(id),"Native Pokemon NBT nature/identity roundtrip "+label);
  JsonObject row=new JsonObject();row.addProperty("source",label);row.addProperty("dex",p.getSpecies().getDex());row.addProperty("form",p.getForm().getName());row.addProperty("nature",nature.name());row.addProperty("role",p.getPersistentData().func_74779_i("BackportLeaderRole"));row.addProperty("moves",p.getMoveset().toString());rows.add(row);members++;
 }
 static void targeted(){
  for(var nature:Nature.values()){
   var p=mon(658,"Water Shuriken","Dark Pulse","Ice Beam","Surf");p.setNature(nature);p.setMintNature(Nature.QUIET);var before=stable(p);NpcTraining.optimizeNature(p);check(p.getNature()==Nature.TIMID,"Special Greninja speed nature from "+nature);check(stable(p).equals(before),"Nature-only training preserves all other build values");
   var snapshot=PokemonFactory.create(p.writeToNBT(new CompoundNBT()));snapshot.setNature(Nature.LONELY);snapshot.setMintNature(Nature.QUIET);NpcTraining.copyNature(p,snapshot);check(snapshot.getBaseNature()==p.getBaseNature()&&snapshot.getNature()==p.getNature(),"Native cleanup snapshot carries base nature");inspect(p,"greninja/"+nature);
  }
  var room=mon(898,"Glacial Lance","High Horsepower","Swords Dance","Trick Room");room.setForm("icerider");NpcTraining.maximize(room);check(room.getNature()==Nature.BRAVE,"Ice rider retains deliberate room attack nature");inspect(room,"ice-rider-room");
  var mixed=mon(149,"Dragon Claw","Flamethrower","Ice Beam","Extreme Speed");NpcTraining.maximize(mixed);check(mixed.getNature().getDecreasedStat()!=BattleStatsType.SPEED,"Bulky Dragonite does not sacrifice speed without room");inspect(mixed,"mixed-dragonite");
  var author=mon(658,"Water Shuriken","Dark Pulse","Ice Beam","Surf");author.setForm(PermanentAsh.MEGA);AuthorRoster.train(author);check(author.getNature()==Nature.TIMID&&Arrays.stream(author.getEVs().getArray()).allMatch(n->n==252),"Author nature with existing six EV252 rule");inspect(author,"author-mega-ash");
 }
 @SubscribeEvent public static void gui(net.minecraftforge.client.event.GuiOpenEvent e){if(e.getGui() instanceof com.pixelmonmod.pixelmon.client.gui.battles.BattleScreen)e.setCanceled(true);}
 static void template(net.minecraft.entity.player.ServerPlayerEntity player,String path)throws Exception{
  CompoundNBT root;try(var in=Nature110Probe.class.getClassLoader().getResourceAsStream(path)){check(in!=null,"Template present "+path);root=CompressedStreamTools.func_74796_a(in);}templates++;
  for(var raw:root.func_150295_c("entities",10)){
   var nbt=((CompoundNBT)raw).func_74775_l("nbt");if(!nbt.func_74779_i("id").equals("pixelmon:npc_trainer"))continue;
   var npc=new NPCTrainer(player.field_70170_p);npc.func_70020_e(nbt);new GymCities.Events().join(new net.minecraftforge.event.entity.EntityJoinWorldEvent(npc,player.field_70170_p));var theme=GymCities.theme(npc);if(theme==null){npc.func_70106_y();continue;}
   themes.add(theme.name());boolean leader=GymCities.isLeader(npc);if(leader)leaders++;else grunts++;trainers++;
   // Each template uses its real team. initBattle invokes the production startBattle hook.
   var human=new PlayerParticipant(player,StorageProxy.getParty(player).get(0));var side=new TrainerParticipant(npc,1);var b=new BattleController(new BattleParticipant[]{human},new BattleParticipant[]{side},new BattleRules());b.sendMessages=false;
   try{var init=BattleController.class.getDeclaredMethod("initBattle");init.setAccessible(true);init.invoke(b);
    for(var w:side.allPokemon){inspect(w.pokemon,path+"/"+npc.func_200200_C_().getString());check(w.initialCopyOfPokemon.getBaseNature()==w.pokemon.getBaseNature()&&w.initialCopyOfPokemon.getNature()==w.pokemon.getNature(),"Final cleanup snapshot preserves optimized nature");}
   }finally{b.battleEnded=true;NpcTeamKnowledge.end(b);NpcTurnKnowledge.end(b);AuthorForesight.end(b);GymChallenge.end(b);Erosion.end(b);CalamityBattle.end(b);npc.func_70106_y();}
  }
 }
 @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){if(done||e.phase!=TickEvent.Phase.END)return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||server.func_184103_al().func_181057_v().isEmpty()||++ticks<120)return;try{
  var player=server.func_184103_al().func_181057_v().get(0);if(paths==null){targeted();paths=new ArrayList<>();try(var in=Nature110Probe.class.getClassLoader().getResourceAsStream("nature110-templates.json")){for(var p:new JsonParser().parse(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray())paths.add(p.getAsString());}var party=StorageProxy.getParty(player);for(int i=0;i<6;i++)party.set(i,null);party.set(0,mon(133,"Tackle","Protect"));}
  if(cursor<paths.size()){template(player,paths.get(cursor++));if(cursor%100==0)Files.writeString(Path.of("nature110-progress.txt"),cursor+"/"+paths.size()+" trainers="+trainers+" members="+members);return;}
  check(themes.size()==18,"All 18 gym types audited "+themes);check(leaders>0&&grunts>0,"Both leader and grunt paths audited");Files.writeString(Path.of("nature110-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(rows));Files.writeString(Path.of("nature110-pass.txt"),"PASS checks="+checks+" templates="+templates+" trainers="+trainers+" leaders="+leaders+" grunts="+grunts+" members="+members+" themes="+themes);done=true;
 }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("nature110-fail.txt"),t.toString());Files.writeString(Path.of("nature110-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(rows));}catch(Exception ignored){}done=true;}}
 @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.func_71410_x();if(done){mc.func_71400_g();return;}mc.field_71474_y.field_82881_y=false;if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}if(!opened&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_("backport-qa-single");}if(mc.field_71439_g!=null){com.pixelmonmod.pixelmon.client.ServerStorageDisplay.showStarterScreen=false;mc.func_147108_a(null);}}
}
