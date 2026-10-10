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
        private static final RenderType FACETS=new RenderType("backport_tera_facets",DefaultVertexFormats.field_181706_f,4,512,false,false,()->{
            RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableTexture();RenderSystem.enableDepthTest();RenderSystem.disableCull();
        },()->{RenderSystem.enableTexture();RenderSystem.enableCull();RenderSystem.disableBlend();}){};
        public CrystalRenderer(EntityRendererManager manager){super(manager);}
        @Override public void render(DenEntity raw,float yaw,float partial,MatrixStack matrix,IRenderTypeBuffer buffer,int light){
            TeraDenEntity den=(TeraDenEntity)raw;int color=TeraRaidClientColors.COLORS[den.teraType()];
            matrix.func_227860_a_();Matrix4f transform=matrix.func_227866_c_().func_227870_a_();IVertexBuilder vertices=buffer.getBuffer(FACETS);
            for(int i=0;i<6;i++){
                double a=i*Math.PI/3,b=(i+1)*Math.PI/3;
                float ax=(float)Math.cos(a)*.7f,az=(float)Math.sin(a)*.7f,bx=(float)Math.cos(b)*.7f,bz=(float)Math.sin(b)*.7f;
                int r=(color>>16)&255,g=(color>>8)&255,bl=color&255;float shade=.65f+.06f*i;
                triangle(vertices,transform,ax,.8f,az,bx,.8f,bz,0,2.1f,0,(int)(r*shade),(int)(g*shade),(int)(bl*shade));
                triangle(vertices,transform,bx,.8f,bz,ax,.8f,az,0,.08f,0,(int)(r*.5),(int)(g*.5),(int)(bl*.5));
            }
            matrix.func_227865_b_();super.render(raw,yaw,partial,matrix,buffer,light);
        }
        private static void triangle(IVertexBuilder v,Matrix4f m,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,int r,int g,int b){
            v.func_227888_a_(m,ax,ay,az).func_225586_a_(r,g,b,230).func_181675_d();v.func_227888_a_(m,bx,by,bz).func_225586_a_(r,g,b,230).func_181675_d();v.func_227888_a_(m,cx,cy,cz).func_225586_a_(r,g,b,230).func_181675_d();
        }
    }
}
