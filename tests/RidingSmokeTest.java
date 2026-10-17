import java.nio.file.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import local.pixelmon.backport.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/** Private integration check, excluded from the distributed map helper and core. */
@Mod.EventBusSubscriber(modid="backport_riding_test")
public class RidingSmokeTest {
    static volatile int phase=-1,age;static volatile boolean finished,rendered;static PixelmonEntity mount;
    static double startY,peak,hoverY;static float width,height,playerWidth,playerHeight;static final JsonArray cases=new JsonArray();
    static boolean enabled(){return Files.isRegularFile(Path.of("riding-smoke-test"));}
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void pass(String name){cases.add(name);System.out.println("RIDING_SMOKE_PASS "+name);}
    static void next(int p){phase=p;age=0;}
    static int mask(){return phase==0||phase==4?RidingMotion.UP:phase==2?RidingMotion.DOWN:phase==5&&age<4?RidingMotion.UP:phase==6&&age<8?RidingMotion.DOWN:0;}
    static void result(boolean success,String error){try{JsonObject r=new JsonObject();r.addProperty("passed",success);r.add("cases",cases);if(error!=null)r.addProperty("error",error);Files.writeString(Path.of("riding-smoke-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(r));}catch(Exception ignored){}finished=true;}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||finished)return;var s=ServerLifecycleHooks.getCurrentServer();if(s==null||s.func_184103_al().func_181057_v().isEmpty())return;
        ServerPlayerEntity p=s.func_184103_al().func_181057_v().get(0);if(!RidingTestMap.map(p)||p.field_70173_aa<140)return;
        try{
            if(phase<0){mount=RidingTestMap.select(p,0,true);check(RidingMotion.active(mount)&&RidingMotion.canFly(mount),"Native flying mount eligibility");width=mount.func_213311_cf();height=mount.func_213302_cg();playerWidth=p.func_213311_cf();playerHeight=p.func_213302_cg();startY=mount.func_226278_cu_();next(0);return;}
            age++;
            if(phase<=4)check(mount.func_213311_cf()==width&&mount.func_213302_cg()==height&&p.func_213311_cf()==playerWidth&&p.func_213302_cg()==playerHeight,"Collision dimensions changed");
            if(phase==0&&age==24){check(mount.isFlying()&&mount.func_226278_cu_()>startY+.8,"Immediate ascent failed y="+mount.func_226278_cu_()+" flying="+mount.isFlying());pass("server-input-immediate-ascent-original-boxes");hoverY=mount.func_226278_cu_();next(1);}
            else if(phase==1&&age==24){check(Math.abs(mount.func_226278_cu_()-hoverY)<.8,"Release did not hover");pass("release-hover-no-charge-release-surge");next(2);}
            else if(phase==2){if(!mount.isFlying()&&mount.func_233570_aj_()){pass("descend-land");startY=mount.func_226278_cu_();next(3);}else check(age<160,"Could not land");}
            else if(phase==3&&age==22){check(!mount.isFlying()&&Math.abs(mount.func_226278_cu_()-startY)<.1,"Descend release relaunched");pass("landing-stays-grounded-on-release");mount.func_70107_b(35,65,0);mount.func_213293_j(0,0,0);next(4);}
            else if(phase==4&&age==28){check(mount.func_174813_aQ().field_72337_e<=68.01,"Passed through roof");pass("low-roof-native-collision");mount=RidingTestMap.select(p,1,true);check(RidingMotion.active(mount)&&!RidingMotion.canFly(mount),"Native ground eligibility");startY=peak=mount.func_226278_cu_();next(5);}
            else if(phase==5){peak=Math.max(peak,mount.func_226278_cu_());check(!mount.isFlying(),"Ground mount gained flight");if(age==35){check(peak>startY+.15,"Ground jump did not start on press");pass("ground-press-jump-no-flight");mount=RidingTestMap.select(p,2,true);check(RidingMotion.active(mount)&&mount.canSurf(),"Native surf eligibility");next(6);}}
            else if(phase==6&&age==24){check(p.func_184187_bx()==mount&&!mount.isFlying(),"Surf control lost mount or gained flight");pass("surf-remains-native-eligible");var cap=StorageProxy.getParty(p).get(5);check(!cap.getForm().getMovement().isRideable(),"Cap eligibility changed");pass("cap-remains-unrideable");mount=RidingTestMap.select(p,0,true);next(7);}
            else if(phase==7){if(rendered){pass("animated-model-seat-and-client-no-horse-bar");p.func_233575_bb_();next(8);}else check(age<160,"No rendered seat preview");}
            else if(phase==8&&age==5){check(!RidingMotion.active(mount),"Dismount state retained");pass("dismount-cleanup");result(true,null);}
        }catch(Throwable failure){failure.printStackTrace();result(false,failure.toString());}
    }
    @Mod.EventBusSubscriber(modid="backport_riding_test",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class Client {
        static int sequence=1000000,closing,renderAge;static boolean captured;
        @SubscribeEvent(priority=EventPriority.LOWEST) public static void tick(TickEvent.ClientTickEvent event){
            if(event.phase!=TickEvent.Phase.END||!enabled())return;var mc=net.minecraft.client.Minecraft.func_71410_x();if(finished){if(Boolean.getBoolean("backport.riding.qa.exit")&&++closing==40)mc.func_71400_g();return;}
            var p=mc.field_71439_g;if(p==null||!RidingMotion.active(p.func_184187_bx()))return;
            PixelmonEntity e=(PixelmonEntity)p.func_184187_bx();local.pixelmon.backport.client.RidingClient.sendInput(e,mask());
            if(phase==7){mc.field_71474_y.func_243229_a(net.minecraft.client.settings.PointOfView.THIRD_PERSON_BACK);p.field_70125_A=12;
                if(++renderAge>35&&!captured)try{
                    check(!p.func_110317_t(),"Horse charge UI remains");var offset=local.pixelmon.backport.client.RidingSeat.offset(p,1);check(offset.func_189985_c()>.00001,"Model seat not sampled");
                    Path shot=Path.of("riding-preview.png");try(var image=net.minecraft.util.ScreenShotHelper.func_198052_a(mc.func_228018_at_().func_198105_m(),mc.func_228018_at_().func_198083_n(),mc.func_147110_a())){image.func_209270_a(shot);}
                    Files.writeString(Path.of("riding-seat-offset.json"),"["+offset.field_72450_a+","+offset.field_72448_b+","+offset.field_72449_c+"]");captured=true;rendered=true;
                }catch(Throwable failure){failure.printStackTrace();result(false,failure.toString());}
            }
        }
    }
}
