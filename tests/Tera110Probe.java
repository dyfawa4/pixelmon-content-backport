package qa;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper;
import com.pixelmonmod.pixelmon.comm.packetHandlers.ReplaceMovePacket;
import com.pixelmonmod.pixelmon.items.TechnicalMoveItem;
import com.pixelmonmod.pixelmon.enums.TMType;
import local.pixelmon.backport.*;
@Mod("backport_tera110_probe") @Mod.EventBusSubscriber(modid="backport_tera110_probe")
public class Tera110Probe {
 static boolean opened,hidden,started,done;static int ticks,age,step;static volatile int request=-1,sent=-1;static volatile UUID target;static volatile String attack="Tera Blast";static Pokemon mon;static ItemStack stack;static int initial;static boolean nativeReuse;
 public static final class Veto { @SubscribeEvent public void cancel(com.pixelmonmod.pixelmon.api.events.npc.TeachMoveEvent.CanLearnMove e){if(TeraMachineLearning.isTera(e.getAttack()))e.setCanceled(true);} }
 static String confirmText(){try{var type=com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.yesNo.YesNoReplaceMove.class;var unsafe=Class.forName("sun.misc.Unsafe");var f=unsafe.getDeclaredField("theUnsafe");f.setAccessible(true);Object receiver=unsafe.getMethod("allocateInstance",Class.class).invoke(f.get(null),type);for(var m:type.getDeclaredMethods())if(m.getName().contains("backport$replaceWords")){m.setAccessible(true);return (String)m.invoke(receiver,"gui.yesno.replace",new Object[]{"旧招","新招"});}throw new AssertionError("Text redirect missing");}catch(ReflectiveOperationException e){throw new RuntimeException(e);}}
 static final Veto veto=new Veto();
 static void check(boolean ok,String reason){if(!ok)throw new AssertionError("case="+step+": "+reason);}
 static void fail(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("tera110-fail.txt"),t.toString());}catch(Exception ignored){}done=true;}
 static Pokemon create(int dex){var p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(50);p.initialize();p.getMoveset().clear();for(String move:new String[]{"Tackle","Protect","Rest","Sleep Talk"})p.getMoveset().add(new Attack(move));return p;}
 static JsonElement normalized(Object value){
  if(value instanceof com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack)return new JsonPrimitive(((com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack)value).getAttackName());
  if(value instanceof com.pixelmonmod.pixelmon.enums.technicalmoves.ITechnicalMove)return new JsonPrimitive(((com.pixelmonmod.pixelmon.enums.technicalmoves.ITechnicalMove)value).getAttackName());
  if(value instanceof Map){JsonObject out=new JsonObject();for(var entry:((Map<?,?>)value).entrySet())out.add(String.valueOf(entry.getKey()),normalized(entry.getValue()));return out;}
  if(value instanceof Collection){var sorted=new TreeMap<String,JsonElement>();for(Object v:(Collection<?>)value){var n=normalized(v);if(!n.equals(new JsonPrimitive("Tera Blast")))sorted.put(n.toString(),n);}JsonArray out=new JsonArray();sorted.values().forEach(out::add);return out;}
  return new JsonPrimitive(String.valueOf(value));
 }
 static void dump()throws Exception{
  JsonArray all=new JsonArray();int forms=0;
  for(var species:PixelmonSpecies.getAll())for(var form:species.getForms()){
   var row=new JsonObject();row.addProperty("dex",species.getDex());row.addProperty("species",species.getName());row.addProperty("form",form.getName());var moves=form.getMoves();if(moves==null)continue;
   row.addProperty("learns",moves.canLearn(DisposableTeraDisc.move()));
   JsonObject other=new JsonObject();for(var f:moves.getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())&&!f.getName().equals("distinctMoves")){f.setAccessible(true);other.add(f.getName(),normalized(f.get(moves)));}row.add("otherMoves",other);
   check(moves.getAllLevelUpMoves().stream().noneMatch(TeraMachineLearning::isTera),"level "+row);check(moves.getTutorMoves().stream().noneMatch(TeraMachineLearning::isTera),"tutor "+row);check(moves.getEggMoves().stream().noneMatch(TeraMachineLearning::isTera),"egg "+row);check(moves.getTransferMoves().stream().noneMatch(TeraMachineLearning::isTera),"transfer "+row);
   all.add(row);forms++;
  }
  Files.writeString(Path.of("tera110-forms.json"),new GsonBuilder().setPrettyPrinting().create().toJson(all));System.out.println("TERA110_TABLES "+forms);
 }
 static void begin(ServerPlayerEntity player)throws Exception{
  player.field_71071_by.func_174888_l();LearnMoveController.clearLearnMoves(player);mon=create(step==1?801:step==6?132:step==16?6:792);StorageProxy.getParty(player).set(0,mon);target=mon.getUUID();attack=step==16?"Flamethrower":"Tera Blast";
  if(step==2)mon.getMoveset().clear();
  stack=new ItemStack(Backport.item("tera_blast_disc_single_use"),2);if(step==8||step==9){stack=TechnicalMoveItem.of(TMType.TM9,DisposableTeraDisc.move());stack.func_190920_e(2);var config=com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy.getGeneral().getTMs();var field=config.getClass().getDeclaredField("allowTMReuse");field.setAccessible(true);field.setBoolean(config,step==8);nativeReuse=config.isAllowTMReuse();}
  player.func_184611_a(Hand.MAIN_HAND,stack);initial=stack.func_190916_E();var entity=mon.getOrCreatePixelmon(player.func_71121_q(),4,80,4);
  if(step==12)StorageProxy.getParty(player).set(0,null);
  if(step==15)com.pixelmonmod.pixelmon.Pixelmon.EVENT_BUS.register(veto);
  if(step==16){stack=TechnicalMoveItem.of(TMType.TM9,com.pixelmonmod.pixelmon.enums.technicalmoves.ITechnicalMove.getMoveFor(TMType.TM9,125));stack.func_190920_e(2);player.func_184611_a(Hand.MAIN_HAND,stack);initial=2;}
  if(step==5)mon.getMoveset().set(0,new Attack("Tera Blast"));
  if(step==10){var other=create(6);StorageProxy.getParty(player).set(1,other);LearnMoveController.addLearnMove(player,other.getUUID(),new Attack("Ember").getMove());}
  if(step==7||step==13){if(step==13)LearnMoveController.sendLearnMove(player,mon.getUUID(),new Attack("Tera Blast").getMove(),p->true);else LearnMoveController.sendLearnMove(player,mon.getUUID(),new Attack("Tera Blast").getMove());}
  else if(step==8||step==9||step==16)new com.pixelmonmod.pixelmon.entities.pixelmon.interactions.InteractionTechnicalMove().processInteract(entity,player,Hand.MAIN_HAND,stack);
  else DisposableTeraDisc.interact(entity,player,Hand.MAIN_HAND,stack);
  if(step==4)player.func_184611_a(Hand.MAIN_HAND,ItemStack.field_190927_a);
  if(step==14){var other=create(6);StorageProxy.getParty(player).set(1,other);target=other.getUUID();}
  age=0;request=step;
 }
 @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){if(done||e.phase!=TickEvent.Phase.END)return;var s=ServerLifecycleHooks.getCurrentServer();if(s==null||s.func_184103_al().func_181057_v().isEmpty()||++ticks<100)return;var player=s.func_184103_al().func_181057_v().get(0);try{
  if(!started){dump();started=true;if(Files.exists(Path.of("tera110-dump-only"))){done=true;return;}begin(player);}
  if(ticks>1600)throw new AssertionError("network test timeout");if(sent!=step||++age<20)return;
  boolean expected=step!=3&&step!=4&&step!=6&&step!=7&&step!=12&&step!=13&&step!=14&&step!=15;
  check(mon.getMoveset().hasAttack(new Attack(attack).getMove())==expected,"learning result");
  int expectedCount=initial;if(expected&&step!=5)expectedCount-=step==8||step==9?(nativeReuse?0:1):1;
  check(stack.func_190916_E()==expectedCount,"cost expected="+expectedCount+" actual="+stack.func_190916_E());
  Files.writeString(Path.of("tera110-cases.txt"),"PASS "+step+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
  if(step==15)com.pixelmonmod.pixelmon.Pixelmon.EVENT_BUS.unregister(veto);
  if(++step<=16)begin(player);else{Files.writeString(Path.of("tera110-pass.txt"),"PASS 17 native request/ReplaceMovePacket cases; complete tables exported");done=true;}
 }catch(Throwable t){fail(t);}}
 @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.func_71410_x();if(done){mc.func_71400_g();return;}mc.field_71474_y.field_82881_y=false;if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}if(!opened&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_("backport-qa-single");}
  if(mc.field_71439_g!=null){com.pixelmonmod.pixelmon.client.ServerStorageDisplay.showStarterScreen=false;if(request>=0&&sent!=request){String text=confirmText();if(!text.contains("忘记 旧招")||!text.contains("学会 新招")){fail(new AssertionError("Incorrect replacement translation: "+text));return;}NetworkHelper.sendToServer(new ReplaceMovePacket(target,attack,request==3?-1:0,false));if(request==11)NetworkHelper.sendToServer(new ReplaceMovePacket(target,attack,0,false));sent=request;}mc.func_147108_a(null);}
  if(done)mc.func_71400_g();
 }
}
