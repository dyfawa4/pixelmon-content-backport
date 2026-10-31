package qa;
import java.nio.file.*;import java.util.*;
import net.minecraftforge.fml.common.Mod;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.event.TickEvent;import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraft.client.Minecraft;import net.minecraft.item.ItemStack;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry;
@Mod("backport_quest110_probe") @Mod.EventBusSubscriber(modid="backport_quest110_probe")
public class Quest110Probe {
 static boolean opened,hidden,done;static int ticks,checks;
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static ItemStack ball(String n){return PokeBallRegistry.getPokeBall(n).getValueUnsafe().getBallItem();}
 @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){if(done||e.phase!=TickEvent.Phase.END)return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||server.func_184103_al().func_181057_v().isEmpty()||++ticks<120)return;try{
  var cls=Class.forName("dev.ftb.mods.ftbquests.quest.task.ItemTask");var unsafe=Class.forName("sun.misc.Unsafe");var f=unsafe.getDeclaredField("theUnsafe");f.setAccessible(true);var task=unsafe.getMethod("allocateInstance",Class.class).invoke(f.get(null),cls);
  var item=cls.getField("item");var match=cls.getField("matchNBT");Object no=Arrays.stream(match.getType().getEnumConstants()).filter(v->((Enum<?>)v).name().equals("FALSE")).findFirst().orElseThrow();Object yes=Arrays.stream(match.getType().getEnumConstants()).filter(v->((Enum<?>)v).name().equals("TRUE")).findFirst().orElseThrow();var test=cls.getMethod("test",ItemStack.class);match.set(task,no);
  var radiant=ball("radiant_ball");item.set(task,radiant);String before=radiant.serializeNBT().toString();
  for(var n:PokeBallRegistry.DEFAULT_POKE_BALL_IDS){var offered=ball(n);check(!(Boolean)test.invoke(task,offered),"reject other ball "+n);}
  check((Boolean)test.invoke(task,radiant),"accept exact Radiant");var decorated=radiant.func_77946_l();decorated.func_196082_o().func_74778_a("qa","extra");check((Boolean)test.invoke(task,decorated),"native loose NBT still allows extra decoration");check(!(Boolean)test.invoke(task,ItemStack.field_190927_a),"empty rejected");
  var plain=ball("poke_ball");plain.func_77982_d(null);check(!(Boolean)test.invoke(task,plain),"untyped shared item rejected for Radiant");
  item.set(task,plain);for(var n:List.of("poke_ball","master_ball","radiant_ball"))check((Boolean)test.invoke(task,ball(n)),"intentionally generic ball task preserved "+n);
  item.set(task,radiant);match.set(task,yes);check(!(Boolean)test.invoke(task,decorated),"native strict NBT not bypassed");check((Boolean)test.invoke(task,radiant),"native strict matching preserved");check(before.equals(radiant.serializeNBT().toString()),"target NBT not mutated");
  Files.writeString(Path.of("quest110-pass.txt"),"PASS actual transformed FTB ItemTask.test cases="+checks+"; default ball IDs="+PokeBallRegistry.DEFAULT_POKE_BALL_IDS.size()+"; no production quest/progress edits");
 }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("quest110-fail.txt"),t.toString());}catch(Exception ignored){}}finally{done=true;}}
 @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.func_71410_x();if(done){mc.func_71400_g();return;}mc.field_71474_y.field_82881_y=false;if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}if(!opened&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_("backport-qa-single");}if(mc.field_71439_g!=null){com.pixelmonmod.pixelmon.client.ServerStorageDisplay.showStarterScreen=false;mc.func_147108_a(null);}}
}
