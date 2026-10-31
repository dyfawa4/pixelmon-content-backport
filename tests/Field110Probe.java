package qa;
import java.nio.file.*;import java.util.*;
import net.minecraftforge.fml.common.Mod;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.event.TickEvent;import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraft.client.Minecraft;import net.minecraft.item.ItemStack;import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
@Mod("backport_field110_probe") @Mod.EventBusSubscriber(modid="backport_field110_probe")
public class Field110Probe {
 static boolean opened,hidden,done;static int ticks,checks;
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){if(done||e.phase!=TickEvent.Phase.END)return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||server.func_184103_al().func_181057_v().isEmpty()||++ticks<100)return;try{
  var player=server.func_184103_al().func_181057_v().get(0);var party=StorageProxy.getParty(player);party.unlockMega(true);party.setMegaItem(com.pixelmonmod.pixelmon.enums.EnumMegaItem.BraceletORAS,true);
  for(var form:List.of("ash",PermanentAsh.FORM)){
   var mon=PokemonFactory.create(PixelmonSpecies.fromNationalDex(658));mon.setLevelNum(100);mon.initialize();mon.setForm(form);mon.heal();mon.setHealth(90);mon.setHeldItem(new ItemStack(Backport.item("ash_greninjite")));party.set(0,mon);
   var id=mon.getUUID();var moves=mon.getMoveset().stream().map(a->a.getMove().getAttackName()).collect(java.util.stream.Collectors.toList());var ability=mon.getAbilityName();
   check(FieldMega.target(mon)!=null&&FieldMega.target(mon).isForm(PermanentAsh.MEGA),"dedicated field route "+form);
   mon.setHeldItem(new ItemStack(Backport.item("greninjite")));check(FieldMega.target(mon)==null,"ordinary stone blocked on Ash "+form);mon.setHeldItem(new ItemStack(Backport.item("ash_greninjite")));
   mon.getOrSpawnPixelmon(player);check("started".equals(FieldMega.start(player,id)),"actual field start "+form);
   var list=com.pixelmonmod.pixelmon.entities.pixelmon.helpers.EvolutionQueryList.queryList;var q=list.stream().filter(v->v.pokemonUUID.equals(id)).findFirst().orElseThrow();var method=q.getClass().getDeclaredMethod("doEvoSwitch");method.setAccessible(true);method.invoke(q);q.abort();list.remove(q);
   check(mon.getForm().isForm(PermanentAsh.MEGA)&&mon.getHealth()==90,"native transformation switch "+form);new FieldMega.Events().tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));check(FieldMega.active(mon),"field tick keeps dedicated route");
   FieldMega.restore(mon);check(mon.getForm().isForm(form)&&mon.getHealth()==90&&mon.getAbilityName().equals(ability),"restore original form HP ability");check(mon.getUUID().equals(id)&&mon.getMoveset().stream().map(a->a.getMove().getAttackName()).collect(java.util.stream.Collectors.toList()).equals(moves),"identity moves preserved");check(mon.getHeldItem().func_77973_b()==Backport.item("ash_greninjite"),"stone retained");mon.getPixelmonEntity().ifPresent(entity->entity.func_70106_y());party.set(0,null);
  }
  Files.writeString(Path.of("field110-pass.txt"),"PASS native field cases="+checks+"; native doEvoSwitch invoked directly, not timed animation playback");
 }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("field110-fail.txt"),t.toString());}catch(Exception ignored){}}finally{done=true;}}
 @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.func_71410_x();if(done){mc.func_71400_g();return;}mc.field_71474_y.field_82881_y=false;if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}if(!opened&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_("backport-qa-single");}if(mc.field_71439_g!=null){com.pixelmonmod.pixelmon.client.ServerStorageDisplay.showStarterScreen=false;mc.func_147108_a(null);}}
}
