package local.pixelmon.backport.mixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderSkybox;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import java.util.function.BiConsumer;

/** Supplied background and title art with the native widgets and splash animation. */
@Mixin(value=MainMenuScreen.class,remap=false)
public abstract class JourneyMainMenuMixin {
    @Unique private static final ResourceLocation backport$background=new ResourceLocation("pixelmon_backport","textures/gui/journey_background.png");
    @Unique private static final ResourceLocation backport$title=new ResourceLocation("pixelmon_backport","textures/gui/journey_title.png");

    @Redirect(method="func_230430_a_",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screen/MainMenuScreen;func_238459_a_(IILjava/util/function/BiConsumer;)V"),require=2)
    private void backport$title(MainMenuScreen menu,int x,int y,BiConsumer<Integer,Integer> nativeLogo,MatrixStack matrix,int mouseX,int mouseY,float partial){
        Screen screen=(Screen)(Object)this;
        int width=Math.min(320,Math.max(1,screen.field_230708_k_-24));
        int height=Math.max(1,Math.round(width*577f/3113f));
        Minecraft.func_71410_x().func_110434_K().func_110577_a(backport$title);
        AbstractGui.func_238466_a_(matrix,(screen.field_230708_k_-width)/2,24,width,height,0,0,3113,577,3113,577);
    }

    // The supplied logo already includes JAVA EDITION.
    @Redirect(method="func_230430_a_",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screen/MainMenuScreen;func_238463_a_(Lcom/mojang/blaze3d/matrix/MatrixStack;IIFFIIII)V"),require=1)
    private void backport$edition(MatrixStack matrix,int x,int y,float u,float v,int width,int height,int textureWidth,int textureHeight){}

    @Redirect(method="func_230430_a_",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/RenderSkybox;func_217623_a(FF)V"))
    private void backport$background(RenderSkybox panorama,float delta,float alpha,MatrixStack matrix,int mouseX,int mouseY,float partial){
        Screen screen=(Screen)(Object)this;
        int width=screen.field_230708_k_,height=screen.field_230709_l_;
        // Center-cover the supplied 2560 x 1494 picture without stretching it.
        int sourceWidth=2560,sourceHeight=1494;float u=0,v=0;
        if(width/(float)height>2560f/1494f){sourceHeight=Math.round(2560f*height/width);v=(1494-sourceHeight)/2f;}
        else{sourceWidth=Math.round(1494f*width/height);u=(2560-sourceWidth)/2f;}
        RenderSystem.color4f(1,1,1,alpha);
        Minecraft.func_71410_x().func_110434_K().func_110577_a(backport$background);
        AbstractGui.func_238466_a_(matrix,0,0,width,height,u,v,sourceWidth,sourceHeight,2560,1494);
    }
}
