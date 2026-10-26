package qa;
import java.nio.file.*;
import java.util.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import local.pixelmon.backport.*;
import local.pixelmon.backport.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.client.settings.PointOfView;
/** Disposable real mounted movement and camera regression, baseline mode records before-fix evidence. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="backport_update180_probe")
public final class Update180RidingRepairCases {
    static PixelmonEntity mount;static volatile int phase;static float hp,riderHP;static boolean damaged;static int accidental;
    static int clientAge;
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void perspective(net.minecraftforge.event.TickEvent.RenderTickEvent event){if(event.phase==net.minecraftforge.event.TickEvent.Phase.START&&Files.exists(Path.of("ride-repair-only"))){var mc=Minecraft.func_71410_x();mc.field_71474_y.func_243229_a(clientAge>=235&&clientAge<250||clientAge>=315?PointOfView.THIRD_PERSON_FRONT:PointOfView.FIRST_PERSON);}}
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent event){
        if(mount!=null&&(event.getEntityLiving()==mount||event.getEntityLiving()==mount.func_184179_bs())){System.out.println("RIDEREPAIR_HURT "+event.getEntityLiving().getClass().getSimpleName()+" source="+event.getSource().func_76355_l()+" amount="+event.getAmount()+" phase="+phase);if(phase<=1)accidental++;}
    }
    public static void setup(ServerPlayerEntity p){
        var world=p.func_71121_q();for(int x=68;x<=125;x++)for(int z=68;z<=125;z++)world.func_180501_a(new BlockPos(x,73,z),Blocks.field_180398_cJ.func_176223_P(),2);
        for(int x=104;x<=124;x++)for(int z=104;z<=124;z++)for(int y=70;y<=75;y++)world.func_180501_a(new BlockPos(x,y,z),(y==70?Blocks.field_150348_b:Blocks.field_150355_j).func_176223_P(),2);
        p.func_71033_a(net.minecraft.world.GameType.SURVIVAL);p.func_70606_j(p.func_110138_aP());
        phase=0;select(p,130,"",80,74,80);System.out.println("RIDEREPAIR_SETUP gyarados survival ground/water, Eternatus normal/Eternamax first/third views");
    }
    static void select(ServerPlayerEntity p,int dex,String form,double x,double y,double z){
        p.func_233575_bb_();if(mount!=null){RidingMotion.clear(mount);mount.func_70106_y();}
        var party=StorageProxy.getParty(p);for(int i=0;i<6;i++)party.set(i,null);var mon=Update180Cases.mon(dex,"Tackle");if(!form.isEmpty())mon.setForm(form);if(form.equals("eternamax"))mon.setHeldItem(new net.minecraft.item.ItemStack(Backport.item("eternatus_meteorite")));mon.heal();party.set(0,mon);
        p.func_200619_a(p.func_71121_q(),x,y,z+3,0,0);mount=mon.getOrCreatePixelmon(p.func_71121_q(),x,y,z);mount.func_70012_b(x,y,z,0,0);mount.func_213293_j(0,0,0);mount.func_94061_f(true);mount.func_70606_j(mount.func_110138_aP());
        if(p.func_71121_q().func_73045_a(mount.func_145782_y())==null)p.func_71121_q().func_217376_c(mount);
        p.field_71135_a.func_147359_a(new net.minecraft.network.play.server.SEntityTeleportPacket(mount));check(p.func_184205_a(mount,true),"Owned mount boards");mount.func_184232_k(p);p.field_71135_a.func_147359_a(new net.minecraft.network.play.server.SSetPassengersPacket(mount));
        hp=mount.func_110143_aJ();riderHP=p.func_110143_aJ();accidental=0;check(RidingMotion.active(mount),"Native ride eligible "+dex+form);
        if(dex==890){mount.setFlying(true);mount.func_189654_d(true);}
        System.out.println("RIDEREPAIR_PHASE "+phase+" dex="+dex+" form="+form+" hp="+hp+" riderHP="+riderHP+" fly="+mount.canFly()+" surf="+mount.canSurf());
    }
    static void measure(ServerPlayerEntity p,String name,boolean synthetic){
        float before=mount.func_110143_aJ();if(synthetic){mount.field_191988_bg=1;mount.func_213293_j(0,0,0);mount.field_70737_aN=0;mount.func_225503_b_(.2f,1);}
        float now=mount.func_110143_aJ(),player=p.func_110143_aJ();boolean lost=now<hp||player<riderHP;damaged|=lost;
        System.out.println("RIDEREPAIR_DAMAGE "+name+" naturalMount="+before+" after="+now+" initial="+hp+" rider="+player+" initialRider="+riderHP+" sourceProbe="+synthetic+" lost="+lost);
        if(!Files.exists(Path.of("ride-baseline")))check(!lost&&accidental==0,"Safe riding loses HP: "+name+" damageEvents="+accidental);
        if(synthetic&&!Files.exists(Path.of("ride-baseline"))){
            phase=9;mount.field_70172_ad=0;mount.func_213293_j(0,-1.4,0);float initial=mount.func_110143_aJ();mount.func_225503_b_(9,1);check(mount.func_110143_aJ()<initial,"Real vertical impact still damages mounted Pokemon");mount.func_70606_j(hp);mount.func_213293_j(0,0,0);
            mount.field_70172_ad=0;initial=mount.func_110143_aJ();mount.func_70097_a(net.minecraft.util.DamageSource.field_76377_j,2);check(mount.func_110143_aJ()<initial,"Ordinary damage still applies while mounted");mount.func_70606_j(hp);System.out.println("RIDEREPAIR_REAL_DAMAGE_PASS vertical impact and ordinary sources preserved");phase=0;
        }
    }
    static void server(Minecraft mc,java.util.function.Consumer<ServerPlayerEntity> action){mc.func_71401_C().func_213167_f(()->{try{action.accept(mc.func_71401_C().func_184103_al().func_181057_v().get(0));}catch(Throwable e){e.printStackTrace();Update180Probe.report("UPDATE180_FAIL riding server "+e);}});}
    public static void client(Minecraft mc,int age)throws Exception{
        clientAge=age;
        if(mc.field_71462_r!=null)mc.func_147108_a(null);mc.field_71439_g.field_70177_z=0;mc.field_71439_g.field_70125_A=0;
        mc.field_71474_y.func_243229_a(age>=235&&age<250||age>=315?PointOfView.THIRD_PERSON_FRONT:PointOfView.FIRST_PERSON);
        if(mc.field_71439_g.func_184187_bx() instanceof PixelmonEntity){var e=(PixelmonEntity)mc.field_71439_g.func_184187_bx();RidingClient.sendInput(e,phase<=1&&age>20?RidingMotion.FORWARD:0);}
        if(age==20)server(mc,p->{p.field_71135_a.func_147359_a(new net.minecraft.network.play.server.SEntityTeleportPacket(mount));p.field_71135_a.func_147359_a(new net.minecraft.network.play.server.SSetPassengersPacket(mount));});
        if(age==95)server(mc,p->measure(p,"gyarados_ground",true));
        if(age==100)server(mc,p->{phase=1;select(p,130,"",112,75,108);});
        if(age==170)server(mc,p->measure(p,"gyarados_surf",false));
        if(age==175)server(mc,p->{phase=2;select(p,890,"",80,78,80);});
        if(age==220||age==240||age==300||age==320){
            var rider=mc.field_71439_g;var e=(PixelmonEntity)rider.func_184187_bx();check(e!=null,"Client mount synchronized");
            System.out.println("RIDEREPAIR_CAMERA age="+age+" form="+e.getForm().getName()+" model="+e.getModel().getClass().getSimpleName()+" active="+RidingMotion.active(e)+" scale="+e.getPixelmonScale()+" offset="+RidingSeat.cameraOffset(rider,1,false)+" camera="+mc.field_71460_t.func_215316_n().func_216785_c()+" mount="+e.func_233580_cy_()+" pov="+mc.field_71474_y.func_243230_g()+" detached="+mc.field_71460_t.func_215316_n().func_216770_i()+" distance="+RidingSeat.cameraDistance(rider,4));
            ScreenShotHelper.func_148259_a(mc.field_71412_D,"qa-180-ride-"+age+".png",mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});
        }
        if(age==250)server(mc,p->{phase=3;select(p,890,"eternamax",80,78,80);});
        if(age==330){System.out.println("RIDEREPAIR_FINISH observedDamage="+damaged+" baseline="+Files.exists(Path.of("ride-baseline")));mc.func_71400_g();}
    }
}
