package qa;
import java.nio.file.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import local.pixelmon.backport.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
@Mod.EventBusSubscriber(modid="backport_update180_probe")
public final class Update180RidingMobileCases {
    static int stage=-1,ticks;static volatile boolean done;static com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity mount;static double x,y,z,peak;
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void select(net.minecraft.entity.player.ServerPlayerEntity p,String form){
        var mon=Update180Cases.mon(484,"Tackle");if(!form.isEmpty())mon.setForm(form);StorageProxy.getParty(p).set(0,mon);mount=Update180RidingMap.select(p,0,true);x=mount.func_226277_ct_();y=peak=mount.func_226278_cu_();z=mount.func_226281_cx_();ticks=0;System.out.println("RIDING_MOBILE_BEGIN stage="+stage+" form="+form+" active="+RidingMotion.active(mount));
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent event){if(event.phase!=TickEvent.Phase.END||!Files.exists(Path.of("riding-audit-only"))||done||!Update180RidingSmoke.finished)return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||server.func_184103_al().func_181057_v().isEmpty())return;var p=server.func_184103_al().func_181057_v().get(0);try{
        if(stage<0){stage=0;select(p,"");return;}ticks++;peak=Math.max(peak,mount.func_226278_cu_());
        if(stage==0&&ticks==45){double distance=Math.hypot(mount.func_226277_ct_()-x,mount.func_226281_cx_()-z);check(distance>1,"Palkia MovementInput-only movement failed distance="+distance);System.out.println("RIDING_MOBILE_PASS standard MovementInput, no desktop key/focus needed; Palkia distance="+distance);stage=1;select(p,"origin");}
        else if(stage==1&&ticks==45){double distance=Math.hypot(mount.func_226277_ct_()-x,mount.func_226281_cx_()-z);check(distance>1&&peak>y+.5&&mount.isFlying(),"Native CInput movement/jump on origin Palkia failed distance="+distance+" rise="+(peak-y));System.out.println("RIDING_MOBILE_PASS native CInputPacket forward+jump; origin Palkia distance="+distance+" rise="+(peak-y));
            p.func_233575_bb_();check(!RidingMotion.active(mount),"Dismount clears active input");
            int samples=0;for(int dex:new int[]{130,484,321}){var mon=Update180Cases.mon(dex,"Tackle");StorageProxy.getParty(p).set(0,mon);mount=Update180RidingMap.select(p,0,true);for(var growth:com.pixelmonmod.pixelmon.enums.EnumGrowth.values()){mon.setGrowth(growth);mount.func_184232_k(p);check(Double.isFinite(p.func_226278_cu_())&&p.func_226278_cu_()>=mount.func_226278_cu_()+.049,"Safe physical seat across growth "+dex+"/"+growth);samples++;}p.func_233575_bb_();}
            System.out.println("RIDING_MOBILE_GROWTH_PASS "+samples+" native growth seats for Gyarados, Palkia, Wailord; dismount cleanup");Files.writeString(Path.of("riding-mobile-result.txt"),"RIDING_MOBILE_PASS "+samples+" growth seats\n");done=true;}
    }catch(Throwable e){e.printStackTrace();try{Files.writeString(Path.of("riding-mobile-result.txt"),"FAIL "+e);}catch(Exception ignored){}done=true;}}
    @Mod.EventBusSubscriber(modid="backport_update180_probe",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class Client {
        static int close;
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END||!Files.exists(Path.of("riding-audit-only")))return;var mc=net.minecraft.client.Minecraft.func_71410_x();if(done){if(++close>30)mc.func_71400_g();return;}if(stage<0||mc.field_71439_g==null)return;mc.field_71439_g.field_70177_z=0;mc.field_71439_g.field_70125_A=0;if(mc.field_71462_r!=null)mc.func_147108_a(null);
            // Model the native touch/controller path without pressing keyboard bindings.
            mc.field_71439_g.field_71158_b.field_192832_b=stage==0?1:0;
            if(stage==1)mc.field_71439_g.field_71174_a.func_147297_a(new net.minecraft.network.play.client.CInputPacket(0,1,ticks<10,false));
        }
    }
}
