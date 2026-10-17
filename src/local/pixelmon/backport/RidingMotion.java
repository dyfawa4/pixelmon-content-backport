package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.client.models.smd.AnimationType;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.vector.Vector3d;

/** Native collision movement, independent of horse travel/charged jump and flight key handlers. */
public final class RidingMotion {
    public static final int FORWARD=1,BACK=2,LEFT=4,RIGHT=8,UP=16,DOWN=32,SPRINT=64;
    private static final Map<PixelmonEntity,State> STATES=Collections.synchronizedMap(new WeakHashMap<>());
    private RidingMotion(){}
    private static final class State {
        UUID rider;String dimension;int mask,inputTick,sequence;boolean sequenced,jumpHeld,gravity;float yaw,pitch,heading;
        State(PixelmonEntity e,PlayerEntity p){rider=p.func_110124_au();dimension=e.field_70170_p.func_234923_W_().func_240901_a_().toString();gravity=e.func_189652_ae();heading=yaw=p.field_70177_z;pitch=p.field_70125_A;}
    }
    public static boolean active(Entity entity){
        if(!(entity instanceof PixelmonEntity)||!entity.func_70089_S())return false;
        PixelmonEntity e=(PixelmonEntity)entity;
        if(!(e.func_184179_bs() instanceof PlayerEntity)||e.getPokemon()==null||e.getForm()==null)return false;
        PlayerEntity p=(PlayerEntity)e.func_184179_bs();
        var config=PixelmonConfigProxy.getGeneral().getRiding();
        return p.func_70089_S()&&p.func_184187_bx()==e&&!p.func_175149_v()&&config.isAllowRiding()
            // The client Pokemon has no server party storage. Use the native owner UUID
            // accessor, which reads synchronized ownership on the client and storage on server.
            &&e.getForm().getMovement().isRideable()&&p.func_110124_au().equals(e.func_184780_dh())
            &&(config.isLandMount()||e.canFly()||e.canSurf());
    }
    public static boolean canFly(PixelmonEntity e){return e.canFly()&&e.getForm().getMovement().getMountedFlyingParameters()!=null;}
    private static State state(PixelmonEntity e){
        PlayerEntity p=(PlayerEntity)e.func_184179_bs();State s=STATES.get(e);
        String dimension=e.field_70170_p.func_234923_W_().func_240901_a_().toString();
        if(s==null||!s.rider.equals(p.func_110124_au())||!s.dimension.equals(dimension)){clear(e);s=new State(e,p);STATES.put(e,s);}return s;
    }
    public static void localInput(PixelmonEntity e,int mask){if(active(e)){State s=state(e);s.mask=mask&127;s.inputTick=e.field_70173_aa;}}
    public static void input(ServerPlayerEntity p,RidingNetwork.Input input){
        if(p==null||!active(p.func_184187_bx()))return;
        PixelmonEntity e=(PixelmonEntity)p.func_184187_bx();
        if(!e.func_110124_au().equals(input.mount)||e.func_145782_y()!=input.entity||BattleRegistry.getBattle(p)!=null)return;
        State s=state(e);if(s.sequenced&&input.sequence-s.sequence<=0)return;
        if(!Float.isFinite(input.yaw)||!Float.isFinite(input.pitch))return;
        s.sequenced=true;s.sequence=input.sequence;s.mask=input.mask;s.inputTick=e.field_70173_aa;s.yaw=MathHelper.func_76142_g(input.yaw);s.pitch=MathHelper.func_76131_a(input.pitch,-90,90);
        p.field_70177_z=s.yaw;p.field_70125_A=s.pitch;
    }
    public static void clear(PixelmonEntity e){State old=STATES.remove(e);if(old!=null){e.func_189654_d(old.gravity);e.setStrafeUpDown(0);e.setFlyPower(0);e.takeOffTicks=0;}}
    public static void cleanup(PixelmonEntity e){if(!active(e))clear(e);}
    public static boolean travel(PixelmonEntity e){
        if(!active(e)){clear(e);return false;}
        // One physics authority. All clients interpolate the server snapshots;
        // the local mount must not run a second integrator against delayed state.
        if(e.field_70170_p.field_72995_K)return true;
        PlayerEntity rider=(PlayerEntity)e.func_184179_bs();State s=state(e);
        int mask=e.field_70173_aa-s.inputTick<=10?s.mask:0;
        if(BattleRegistry.getBattle(rider)!=null||BattleMotion.frozen(rider)){e.func_213293_j(0,0,0);s.jumpHeld=false;sync(e,s);return true;}
        boolean up=(mask&UP)!=0,down=(mask&DOWN)!=0,pressed=up&&!s.jumpHeld;s.jumpHeld=up;
        boolean flying=e.isFlying()&&canFly(e),surf=e.func_70090_H()&&e.canSurf();
        if(pressed&&!down&&canFly(e)&&!flying){e.setFlying(true);e.setAnimation(AnimationType.FLY);flying=true;e.takeOffTicks=0;e.setFlyPower(0);}
        if(!canFly(e)&&e.isFlying()){e.setFlying(false);flying=false;}
        e.func_189654_d(flying);
        s.heading=MathHelper.func_76142_g(s.heading+MathHelper.func_76131_a(MathHelper.func_76142_g(s.yaw-s.heading)*.5f,-30,30));
        float yaw=s.heading;e.field_70177_z=yaw;e.field_70761_aq=yaw;e.field_70759_as=yaw;
        e.field_70125_A=flying?MathHelper.func_76131_a(s.pitch*.12f,-8,8):0;
        e.prevRotationRoll=e.rotationRoll;e.rotationRoll*=.65f;
        float forward=((mask&FORWARD)!=0?1:0)-((mask&BACK)!=0?1:0);
        float strafe=((mask&LEFT)!=0?1:0)-((mask&RIGHT)!=0?1:0);
        double length=Math.sqrt(forward*forward+strafe*strafe);if(length>1){forward/=length;strafe/=length;}
        double stat=Math.min(500,Math.max(1,e.getPokemon().getStat(BattleStatsType.SPEED)));
        double speed=(flying?.48+stat/1000:surf?.34+stat/1600:.30+stat/1600)*PixelmonConfigProxy.getGeneral().getRiding().getRidingSpeedMultiplier();
        if((mask&SPRINT)!=0)speed*=1.3;
        if(flying)speed=Math.min(speed,Math.max(.05,PixelmonConfigProxy.getGeneral().getRiding().getFlyingSpeedLimit()));
        double angle=Math.toRadians(yaw),dx=(strafe*Math.cos(angle)-forward*Math.sin(angle))*speed,dz=(forward*Math.cos(angle)+strafe*Math.sin(angle))*speed;
        Vector3d previous=e.func_213322_ci();double acceleration=flying?.30:surf?.24:.55;
        double vx=previous.field_72450_a+(dx-previous.field_72450_a)*acceleration,vz=previous.field_72449_c+(dz-previous.field_72449_c)*acceleration,vy;
        if(flying){double target=up==down?0:up?.32:-.30;vy=previous.field_72448_b+(target-previous.field_72448_b)*.5;e.field_70143_R=0;}
        else if(surf){double target=up==down?surfaceBuoyancy(e):up?.22:-.22;vy=previous.field_72448_b+(target-previous.field_72448_b)*.4;}
        else{
            if(pressed&&e.func_233570_aj_())e.doJump();
            vy=e.func_213322_ci().field_72448_b;
            if(e.func_233570_aj_()&&!(pressed&&vy>0))vy=-.08;
        }
        e.func_70637_d(false);e.field_70702_br=strafe;e.field_191988_bg=forward;e.field_70701_bs=0;
        e.func_213293_j(vx,vy,vz);e.func_213315_a(MoverType.SELF,e.func_213322_ci());
        Vector3d moved=e.func_213322_ci();
        if(flying&&e.func_233570_aj_()&&vy<=0){e.onLanding();e.func_189654_d(false);flying=false;e.func_213293_j(moved.field_72450_a,-.08,moved.field_72449_c);}
        else if(!flying&&!surf)e.func_213293_j(moved.field_72450_a,(moved.field_72448_b-.08)*.98,moved.field_72449_c);
        // Keep the existing walk/fly animation and native world collision; never resize or teleport.
        e.field_70721_aZ=(float)Math.min(1,Math.sqrt(vx*vx+vz*vz)*3);sync(e,s);return true;
    }
    private static void sync(PixelmonEntity e,State s){
        if(!e.field_70170_p.field_72995_K&&e.field_70173_aa%2==0&&e.func_184179_bs() instanceof ServerPlayerEntity)
            RidingNetwork.send((ServerPlayerEntity)e.func_184179_bs(),new RidingNetwork.Snapshot(e,s.sequence));
    }
    private static double surfaceBuoyancy(PixelmonEntity e){
        BlockPos pos=e.func_233580_cy_();if(!e.field_70170_p.func_204610_c(pos).func_206884_a(FluidTags.field_206959_a))return 0;
        for(int i=0;i<16&&e.field_70170_p.func_204610_c(pos.func_177984_a()).func_206884_a(FluidTags.field_206959_a);i++)pos=pos.func_177984_a();
        double target=pos.func_177956_o()+1-Math.min(.6,e.func_213302_cg()*.25);
        return Math.max(-.12,Math.min(.12,(target-e.func_226278_cu_())*.18));
    }
}
