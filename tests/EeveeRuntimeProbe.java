package qa;
import java.nio.file.*;import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;import com.pixelmonmod.pixelmon.advancements.PixelmonAdvancements;
import net.minecraftforge.fml.common.Mod;import net.minecraftforge.api.distmarker.Dist;import net.minecraftforge.event.TickEvent;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.fml.server.ServerLifecycleHooks;
@Mod("eevee_runtime_probe") @Mod.EventBusSubscriber(modid="eevee_runtime_probe")
public class EeveeRuntimeProbe {
 static final String WORLD="backport-eevee-check";static volatile boolean done;static int stage,ticks;static long deadline=System.nanoTime()+180_000_000_000L;
 static boolean enabled(){if(!WORLD.equals(System.getProperty("backport.manual.world")))return false;var s=ServerLifecycleHooks.getCurrentServer();return s==null||s.func_240776_a_(net.minecraft.world.storage.FolderName.field_237253_i_).normalize().getFileName().toString().equals(WORLD);}
 static void log(String m){System.out.println("EEVEE_RUNTIME "+m);try{Files.writeString(Path.of("eevee-runtime-result.txt"),m+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}}
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static Pokemon mon(int dex){return PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));}
 static Set<String> earned(net.minecraft.entity.player.ServerPlayerEntity p,net.minecraft.advancements.Advancement a){var out=new HashSet<String>();p.func_192039_O().func_192747_a(a).func_192102_e().forEach(out::add);return out;}
 static void verify(net.minecraft.entity.player.ServerPlayerEntity p)throws Exception{
 var s=ServerLifecycleHooks.getCurrentServer();var a=s.func_191949_aK().func_192778_a(new net.minecraft.util.ResourceLocation("pixelmon:specs/eeveelution_capture_all"));check(a!=null,"missing runtime advancement");check(a.func_192073_f().size()==9,"runtime criterion count");check(a.func_192074_h().length==9,"runtime AND row count");for(var row:a.func_192074_h())check(row.length==1,"runtime OR instead of AND");
 int[] dex={133,134,135,136,196,197,470,471,700};for(int i=0;i<9;i++){var inst=a.func_192073_f().get(""+i).func_192143_a();check(inst instanceof local.pixelmon.backport.SpeciesCriterionGuard,"runtime mixin missing");var f=inst.getClass().getDeclaredField("backport$expectedDex");f.setAccessible(true);check(f.getInt(inst)==dex[i],"runtime dex metadata missing for "+i);p.func_192039_O().func_192744_b(a,""+i);}
 log("LOADED_GRAPH_PASS stage="+stage+" nine exact species / nine AND rows");
 for(int id:new int[]{1,25,172,383,501,890,1026})PixelmonAdvancements.SPEC_TRIGGER.trigger(p,mon(id));check(earned(p,a).isEmpty(),"unrelated species granted family");
 for(int i=0;i<12;i++)PixelmonAdvancements.SPEC_TRIGGER.trigger(p,mon(133));check(earned(p,a).equals(Set.of("0")),"duplicate Eevee granted other criteria");
 for(int i=1;i<8;i++)PixelmonAdvancements.SPEC_TRIGGER.trigger(p,mon(dex[i]));check(earned(p,a).size()==8&&!p.func_192039_O().func_192747_a(a).func_192105_a(),"eight members completed family");
 PixelmonAdvancements.SPEC_TRIGGER.trigger(p,mon(700).makeEgg());check(earned(p,a).size()==8,"egg granted last member");
 PixelmonAdvancements.SPEC_TRIGGER.trigger(p,mon(700));check(earned(p,a).size()==9&&p.func_192039_O().func_192747_a(a).func_192105_a(),"nine actual species failed");log("NATIVE_TRIGGER_PASS stage="+stage+" wrong species / duplicates / eight / egg rejected; nine accepted");
 }
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(!enabled()||done||e.phase!=TickEvent.Phase.END)return;var s=ServerLifecycleHooks.getCurrentServer();if(s==null||s.func_184103_al().func_181057_v().isEmpty())return;
 try{if(stage==0){var p=s.func_184103_al().func_181057_v().get(0);var party=com.pixelmonmod.pixelmon.api.storage.StorageProxy.getParty(p);party.starterPicked=true;party.guiOpened=true;verify(p);log("PASS startup loaded advancement and native trigger");done=true;}
}
 catch(Throwable ex){ex.printStackTrace();log("FAIL "+ex);done=true;}}
 @Mod.EventBusSubscriber(modid="eevee_runtime_probe",value=Dist.CLIENT) public static class Client{static boolean opened,hidden;static int exit;
 @SubscribeEvent public static void gui(net.minecraftforge.client.event.GuiOpenEvent e){if(enabled()&&e.getGui() instanceof com.pixelmonmod.pixelmon.client.gui.starter.ChooseStarterScreen)e.setCanceled(true);}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(!enabled()||e.phase!=TickEvent.Phase.END)return;var mc=net.minecraft.client.Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}if(!opened&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_(WORLD);}if(System.nanoTime()>deadline&&!done){log("FAIL timeout");done=true;}if(done&&++exit>20)mc.func_71400_g();}}
}
