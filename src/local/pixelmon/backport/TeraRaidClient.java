package local.pixelmon.backport;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pixelmonmod.pixelmon.entities.DenEntity;
import com.pixelmonmod.pixelmon.client.gui.battles.BattleScreen;
import com.pixelmonmod.pixelmon.client.render.entity.renderers.DenRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

/** Client-only crystal geometry and raid status; normal battle HUD stays native. */
@Mod.EventBusSubscriber(modid=Content.NS,value=Dist.CLIENT)
public final class TeraRaidClient {
    public static TeraNetwork.RaidStatus status;
    private TeraRaidClient(){}
    @Mod.EventBusSubscriber(modid=Content.NS,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent public static void setup(FMLClientSetupEvent event){RenderingRegistry.registerEntityRenderingHandler(Backport.TERA_DEN.get(),CrystalRenderer::new);}
        @SubscribeEvent public static void colors(net.minecraftforge.client.event.ColorHandlerEvent.Item event){event.getItemColors().func_199877_a((stack,tint)->0x70deeb,Backport.item("tera_raid_music_disc"));event.getItemColors().func_199877_a((stack,tint)->0x55ccff,Backport.item("ash_greninjite"));}
    }
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent event){status=null;}
    @SubscribeEvent public static void draw(net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post event){
        if(!(event.getGui() instanceof BattleScreen) || status==null)return;
        BattleScreen screen=(BattleScreen)event.getGui();if(screen.bm.battleControllerIndex!=status.battle)return;
        Minecraft mc=Minecraft.func_71410_x();int cx=screen.field_230708_k_/2;
        if(screen.bm.displayedEnemyPokemon==null || screen.bm.displayedEnemyPokemon.length!=1 || screen.bm.displayedEnemyPokemon[0]==null)return;
        // Native OverlayNew draws the raid boss in a 250 x 70 panel at this scale.
        // Anchor below its lower edge so the status never covers its name or HP.
        float scale=screen.field_230708_k_<700?1f/new java.math.BigDecimal(700f/screen.field_230708_k_).setScale(2,java.math.RoundingMode.HALF_UP).floatValue():1f;
        int top=(int)Math.ceil((screen.bm.displayedEnemyPokemon[0].xPos+70)*scale)+4;
        String text="太晶团战  "+status.seconds+"秒  充能 "+status.charge+"/3  护盾 "+status.shield+"/8";
        int w=mc.field_71466_p.func_78256_a(text)+12;
        AbstractGui.func_238467_a_(event.getMatrixStack(),cx-w/2,top,cx+w/2,top+16,0xc0182838);
        mc.field_71466_p.func_238421_b_(event.getMatrixStack(),text,cx-w/2+6,top+4,0xd5f5ff);
    }
    public static final class CrystalRenderer extends DenRenderer {
        public CrystalRenderer(EntityRendererManager manager){super(manager);}
        @Override public void render(DenEntity raw,float yaw,float partial,MatrixStack matrix,IRenderTypeBuffer buffer,int light){
            TeraDenEntity den=(TeraDenEntity)raw;int color=TeraRaidClientColors.COLORS[den.teraType()];
            local.pixelmon.backport.client.TeraDenMesh.render(matrix,buffer,light,color);
            if(den.hasChallenge())super.render(raw,yaw,partial,matrix,buffer,light);
        }
    }
}
