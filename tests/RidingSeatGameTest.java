import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.client.models.PixelmonModelSmd;
import com.pixelmonmod.pixelmon.client.ServerStorageDisplay;
import com.pixelmonmod.pixelmon.client.gui.starter.ChooseStarterScreen;
import local.pixelmon.backport.*;
import local.pixelmon.backport.client.RidingSeat;
import local.pixelmon.backport.client.RidingClient;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraft.client.settings.PointOfView;

/** Private visual fixture. Never included in the distributable helper. Leaves game open. */
@Mod.EventBusSubscriber(modid="backport_riding_test")
public class RidingSeatGameTest {
    static volatile int phase=-1,age;static volatile boolean done;static PixelmonEntity mount;static int prepared=-1;
    static JsonArray records=new JsonArray();static int starterFrames,nativeOnixRemounts;
    static boolean control(){return Files.isRegularFile(Path.of("riding-control-test"));}
    static boolean enabled(){return control()||Files.isRegularFile(Path.of("riding-seat-test"));}
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled())return;if(done){if(mount!=null){RidingMotion.clear(mount);mount=null;}return;}var s=ServerLifecycleHooks.getCurrentServer();if(s==null||s.func_184103_al().func_181057_v().isEmpty())return;
        var p=s.func_184103_al().func_181057_v().get(0);if(!RidingTestMap.map(p)||p.field_70173_aa<40)return;
        if(phase<0)phase=control()?6:0;
        if(prepared!=phase){int index=phase==6||phase==9?0:phase==7?2:phase==8?3:phase<=1?2:phase==2?1:phase==3?0:phase==4?3:4;mount=RidingTestMap.select(p,index,true);if(phase!=7)mount.func_70107_b(0,65,0);prepared=phase;age=0;System.out.println("RIDING_SEAT_TEST_START phase="+phase);}else age++;
        if(phase==8&&(age==5||age==12||age==19)){
            p.func_233575_bb_();p.func_184611_a(net.minecraft.util.Hand.MAIN_HAND,net.minecraft.item.ItemStack.field_190927_a);
            mount.func_230254_b_(p,net.minecraft.util.Hand.MAIN_HAND);
            if(p.func_184187_bx()==mount)nativeOnixRemounts++;
            p.field_71135_a.func_147359_a(new net.minecraft.network.play.server.SSetPassengersPacket(mount));
        }
    }
    @Mod.EventBusSubscriber(modid="backport_riding_test",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class Client {
        static int waitTicks;static boolean captured,shown;static double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY;
        @SubscribeEvent(priority=EventPriority.LOWEST) public static void tick(TickEvent.ClientTickEvent event){
            if(event.phase!=TickEvent.Phase.END||!enabled())return;var mc=net.minecraft.client.Minecraft.func_71410_x();long window=mc.func_228018_at_().func_198092_i();
            if(done){if(!shown){org.lwjgl.glfw.GLFW.glfwShowWindow(window);shown=true;}return;}org.lwjgl.glfw.GLFW.glfwHideWindow(window);
            var p=mc.field_71439_g;if(p==null)return;
            if(++waitTicks%100==0){mc.func_228018_at_().func_230148_b_("轻快之旅 | 乘骑修正 r9 · 简测后保留游戏");System.out.println("RIDING_SEAT_TEST_STATUS phase="+phase+" age="+age+" playerTicks="+p.field_70173_aa+" gui="+(mc.field_71462_r==null?"none":mc.field_71462_r.getClass().getSimpleName()));}
            if(mc.field_71462_r instanceof ChooseStarterScreen)starterFrames++;
            if(mc.field_71462_r!=null)mc.func_147108_a(null);
            if(age>180&&!captured){try{JsonObject error=new JsonObject();error.addProperty("error","Client mount missing or fixture interrupted at phase "+phase);records.add(error);Files.writeString(Path.of("riding-seat-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(records));}catch(Exception ignored){}done=true;return;}
            if(!(p.func_184187_bx() instanceof PixelmonEntity)||phase<0)return;PixelmonEntity e=(PixelmonEntity)p.func_184187_bx();
            int mask=phase==9&&age<25?RidingMotion.UP:phase==6?RidingMotion.FORWARD|(age<25?RidingMotion.UP:0):phase==7&&age<16?RidingMotion.FORWARD:phase==8?RidingMotion.FORWARD:phase==2?RidingMotion.FORWARD|(age<4?RidingMotion.UP:0):0;
            p.field_70125_A=0;p.field_70177_z=phase==8||(phase==6||phase==9)&&age>30?90:0;
            RidingClient.sendInput(e,mask);
            mc.field_71474_y.func_243229_a(phase==0||phase==5||phase==6||phase==7?PointOfView.FIRST_PERSON:phase>=3?PointOfView.THIRD_PERSON_FRONT:PointOfView.THIRD_PERSON_BACK);
            if(age>35){var o=RidingSeat.offset(p,1);double y=p.func_226278_cu_()+o.field_72448_b;low=Math.min(low,y);high=Math.max(high,y);}
            if(age>=65&&!captured)try{
                JsonObject r=new JsonObject();r.addProperty("phase",phase);r.addProperty("model",e.getModel().getClass().getName());r.addProperty("active",RidingMotion.active(e));r.addProperty("offset",RidingSeat.offset(p,1).toString());r.addProperty("player",p.func_233580_cy_().toString());r.addProperty("mountY",e.func_226278_cu_());r.addProperty("camera",mc.field_71460_t.func_215316_n().func_216785_c().toString());r.addProperty("seatJitter",high-low);r.addProperty("showStarter",ServerStorageDisplay.showStarterScreen);r.addProperty("starterFrames",starterFrames);
                r.addProperty("playerYaw",p.field_70177_z);r.addProperty("mountYaw",e.field_70177_z);r.addProperty("bodyYaw",e.field_70761_aq);r.addProperty("serverFlying",mount.isFlying());r.addProperty("serverY",mount.func_226278_cu_());r.addProperty("serverX",mount.func_226277_ct_());r.addProperty("serverZ",mount.func_226281_cx_());
                if(phase==8)r.addProperty("nativeOnixRemounts",nativeOnixRemounts);
                if(e.getModel() instanceof PixelmonModelSmd){var m=((PixelmonModelSmd<?>)e.getModel()).theModel;JsonArray bones=new JsonArray();for(var b:m.body.bones)bones.add(b.name+":"+b.verts.size());r.add("bones",bones);}
                records.add(r);Files.writeString(Path.of("riding-seat-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(records));
                try(var image=net.minecraft.util.ScreenShotHelper.func_198052_a(mc.func_228018_at_().func_198105_m(),mc.func_228018_at_().func_198083_n(),mc.func_147110_a())){image.func_209270_a(Path.of("riding-lapras-"+phase+".png"));}
                captured=true;
            }catch(Exception failure){failure.printStackTrace();done=true;}
            if(age>72&&captured){if(phase<(control()?9:5)){phase++;age=0;captured=false;low=Double.POSITIVE_INFINITY;high=Double.NEGATIVE_INFINITY;}else{RidingMotion.clear(e);done=true;}}
        }
    }
}
