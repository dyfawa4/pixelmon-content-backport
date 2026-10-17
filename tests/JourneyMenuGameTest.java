import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Main menu preview/navigation only in a private test client with no world loaded. */
@Mod.EventBusSubscriber(modid="backport_tera_visual_test",value=Dist.CLIENT)
public class JourneyMenuGameTest {
    static int frames,phase;static Screen parent;static final JsonArray cases=new JsonArray();static boolean finished;
    static String key(Button b){return b.func_230458_i_() instanceof TranslationTextComponent?((TranslationTextComponent)b.func_230458_i_()).func_150268_i():b.func_230458_i_().getString();}
    static Button button(Screen screen,String key){for(var child:screen.func_231039_at__())if(child instanceof Button&&key((Button)child).equals(key))return (Button)child;throw new AssertionError("Missing functional menu button "+key);}
    static void screenshot(Minecraft mc,String name)throws Exception{var path=Path.of("screenshots",name+".png");Files.createDirectories(path.getParent());try(var image=net.minecraft.util.ScreenShotHelper.func_198052_a(mc.func_228018_at_().func_198105_m(),mc.func_228018_at_().func_198083_n(),mc.func_147110_a())){image.func_209270_a(path);}System.out.println("JOURNEY_MENU_SCREENSHOT "+path);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||finished||!Files.isRegularFile(Path.of("menu-private-test")))return;
        var mc=Minecraft.func_71410_x();if(mc.field_71462_r==null)return;
        if(parent==null){if(!(mc.field_71462_r instanceof MainMenuScreen))return;parent=mc.field_71462_r;}
        if(++frames%60!=0)return;
        try{
            if(phase<3){
                if(!(mc.field_71462_r instanceof MainMenuScreen))throw new AssertionError("Lost main menu");
                for(var child:parent.func_231039_at__())if(child.getClass().getName().contains("Journey"))throw new AssertionError("Custom widget remains: "+child.getClass().getName());
                for(String required:new String[]{"menu.singleplayer","menu.multiplayer","menu.options","menu.quit","fml.menu.mods","narrator.button.language","narrator.button.accessibility"})button(parent,required);
                screenshot(mc,"journey-menu-"+phase);JsonObject row=new JsonObject();row.addProperty("phase",phase);row.addProperty("guiWidth",parent.field_230708_k_);row.addProperty("guiHeight",parent.field_230709_l_);JsonArray widgets=new JsonArray();for(var child:parent.func_231039_at__())if(child instanceof Button){var b=(Button)child;JsonObject widget=new JsonObject();widget.addProperty("key",key(b));widget.addProperty("class",b.getClass().getName());widget.addProperty("x",b.field_230690_l_);widget.addProperty("y",b.field_230691_m_);widgets.add(widget);}row.add("nativeButtons",widgets);cases.add(row);
                if(phase==0)GLFW.glfwSetWindowSize(mc.func_228018_at_().func_198092_i(),1920,1080);else if(phase==1)GLFW.glfwSetWindowSize(mc.func_228018_at_().func_198092_i(),960,540);phase++;return;
            }
            if(phase==3){button(parent,"menu.singleplayer").func_230930_b_();if(!(mc.field_71462_r instanceof WorldSelectionScreen))throw new AssertionError("Native singleplayer callback lost");mc.func_147108_a(parent);phase++;return;}
            if(phase==4){button(parent,"menu.options").func_230930_b_();if(!(mc.field_71462_r instanceof OptionsScreen))throw new AssertionError("Native options callback lost");mc.func_147108_a(parent);phase++;return;}
            if(phase==5){button(parent,"narrator.button.language").func_230930_b_();if(!(mc.field_71462_r instanceof LanguageScreen))throw new AssertionError("Native language callback lost");mc.func_147108_a(parent);phase++;return;}
            if(phase==6){button(parent,"menu.multiplayer").func_230930_b_();if(mc.field_71462_r==parent)throw new AssertionError("Native multiplayer callback lost");JsonObject row=new JsonObject();row.addProperty("multiplayerTarget",mc.field_71462_r.getClass().getName());cases.add(row);mc.func_147108_a(parent);phase++;return;}
            var result=new JsonObject();result.addProperty("passed",true);result.add("cases",cases);Files.writeString(Path.of("menu-game-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));finished=true;System.out.println("JOURNEY_MENU_GAME_PASS");mc.func_71400_g();
        }catch(Throwable error){finished=true;error.printStackTrace();try{Files.writeString(Path.of("menu-game-result.json"),"{\"passed\":false}");}catch(Exception ignored){}System.out.println("JOURNEY_MENU_GAME_FAIL");mc.func_71400_g();}
    }
}
