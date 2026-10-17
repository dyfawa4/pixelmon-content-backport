package local.pixelmon.backport.client;
import java.util.UUID;
import com.pixelmonmod.pixelmon.client.keybindings.DescendKey;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import local.pixelmon.backport.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=Content.NS,value=Dist.CLIENT)
public final class RidingClient {
    private static UUID lastMount;private static int lastMask=-1,tick,sequence;
    private static float lastYaw,lastPitch;
    private static net.minecraft.client.world.ClientWorld lastWorld;
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event){if(event.phase==TickEvent.Phase.START)RidingSeat.nextFrame();}
    @SubscribeEvent public static void input(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.START)return;
        var mc=Minecraft.func_71410_x();var p=mc.field_71439_g;tick++;
        if(lastWorld!=mc.field_71441_e){lastWorld=mc.field_71441_e;lastMount=null;lastMask=-1;RidingSeat.clear();}
        RidingSeat.tickAll();
        if(p==null||!RidingMotion.active(p.func_184187_bx())){lastMount=null;lastMask=-1;return;}
        PixelmonEntity e=(PixelmonEntity)p.func_184187_bx();var keys=mc.field_71474_y;int mask=0;
        if(mc.field_71462_r==null&&org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(mc.func_228018_at_().func_198092_i(),org.lwjgl.glfw.GLFW.GLFW_FOCUSED)==org.lwjgl.glfw.GLFW.GLFW_TRUE){
            if(keys.field_74351_w.func_151470_d())mask|=RidingMotion.FORWARD;if(keys.field_74368_y.func_151470_d())mask|=RidingMotion.BACK;
            if(keys.field_74370_x.func_151470_d())mask|=RidingMotion.LEFT;if(keys.field_74366_z.func_151470_d())mask|=RidingMotion.RIGHT;
            if(keys.field_74314_A.func_151470_d())mask|=RidingMotion.UP;if(DescendKey.Instance.func_151470_d())mask|=RidingMotion.DOWN;
            if(keys.field_151444_V.func_151470_d())mask|=RidingMotion.SPRINT;
        }
        RidingMotion.localInput(e,mask);
        if(!e.func_110124_au().equals(lastMount)||mask!=lastMask||tick%5==0||Math.abs(net.minecraft.util.math.MathHelper.func_76142_g(p.field_70177_z-lastYaw))>.5f||Math.abs(p.field_70125_A-lastPitch)>.5f)sendInput(e,mask);
    }
    public static void sendInput(PixelmonEntity e,int mask){var p=Minecraft.func_71410_x().field_71439_g;if(p==null)return;RidingMotion.localInput(e,mask);RidingNetwork.CHANNEL.sendToServer(new RidingNetwork.Input(e.func_110124_au(),e.func_145782_y(),++sequence,mask,p.field_70177_z,p.field_70125_A));lastMount=e.func_110124_au();lastMask=mask;lastYaw=p.field_70177_z;lastPitch=p.field_70125_A;}
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent event){lastWorld=null;lastMount=null;lastMask=-1;RidingSeat.clear();}
    @SubscribeEvent public static void reload(net.minecraftforge.client.event.TextureStitchEvent.Pre event){RidingSeat.clear();}
    public static void snapshot(RidingNetwork.Snapshot state){
        var mc=Minecraft.func_71410_x();var p=mc.field_71439_g;if(p==null||!RidingMotion.active(p.func_184187_bx()))return;
        PixelmonEntity e=(PixelmonEntity)p.func_184187_bx();if(e.func_145782_y()!=state.entity||!e.func_110124_au().equals(state.mount))return;
        e.func_180426_a(state.x,state.y,state.z,state.yaw,state.flying?p.field_70125_A*.12f:0,2,false);
        e.field_70761_aq=state.yaw;e.field_70759_as=state.yaw;
        e.func_213293_j(0,0,0);e.setFlying(state.flying);e.func_230245_c_(state.ground);
    }
}
