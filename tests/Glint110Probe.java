package qa;
import java.nio.file.*;import java.util.*;
import net.minecraftforge.fml.common.Mod;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.event.TickEvent;import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraft.client.Minecraft;import net.minecraft.item.ItemStack;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry;
@Mod("backport_glint110_probe") @Mod.EventBusSubscriber(modid="backport_glint110_probe")
public class Glint110Probe {
 static boolean opened,hidden,done;static int ticks,checks;
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static ItemStack ball(String n){return PokeBallRegistry.getPokeBall(n).getValueUnsafe().getBallItem();}
 @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){if(done||e.phase!=TickEvent.Phase.END)return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||server.func_184103_al().func_181057_v().isEmpty()||++ticks<120)return;try{
  var radiant=ball("radiant_ball");String before=radiant.serializeNBT().toString();check(radiant.func_77962_s(),"Radiant uses native stack glint");for(var id:PokeBallRegistry.DEFAULT_POKE_BALL_IDS)check(!ball(id).func_77962_s(),"ordinary ball unchanged "+id);check(before.equals(radiant.serializeNBT().toString()),"no enchantment NBT added");check(new ItemStack(local.pixelmon.backport.Backport.item("ash_greninjite")).func_77962_s(),"existing Mega stone glint retained");Files.writeString(Path.of("glint110-pass.txt"),"PASS native ItemStack glint checks="+checks+"; Radiant only, no NBT change");
 }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("glint110-fail.txt"),t.toString());}catch(Exception ignored){}}finally{done=true;}}
 @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.func_71410_x();if(done){mc.func_71400_g();return;}mc.field_71474_y.field_82881_y=false;if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}if(!opened&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_("backport-qa-single");}if(mc.field_71439_g!=null){com.pixelmonmod.pixelmon.client.ServerStorageDisplay.showStarterScreen=false;mc.func_147108_a(null);}}
}
