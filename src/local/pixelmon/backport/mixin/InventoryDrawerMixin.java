package local.pixelmon.backport.mixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import com.pixelmonmod.pixelmon.client.gui.inventory.*;
import net.minecraft.client.gui.DisplayEffectsScreen;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** The native equipment drawer stays open; all native equipment click paths remain. */
@Mixin(value=InventoryPixelmon.class,remap=false)
public abstract class InventoryDrawerMixin {
    @Shadow private boolean drawerOut;
    @Shadow private int drawerOffset;
    @Shadow private int partyWidth;
    @Shadow private DisplayEffectsScreen gui;
    @Inject(method={"initGui","render","drawGuiContainerBackgroundLayer"},at=@At("HEAD"))
    private void backport$open(CallbackInfo ci){drawerOut=true;drawerOffset=42;}
    @Inject(method="<init>",at=@At("TAIL"))
    private void backport$initiallyOpen(CallbackInfo ci){drawerOut=true;drawerOffset=42;}
    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void backport$noArrow(double x,double y,int button,CallbackInfoReturnable<Boolean> ci){drawerOut=true;drawerOffset=42;int left=((InventoryPixelmonBase)gui).getGUILeft()-partyWidth,mid=gui.field_230709_l_/2;if(x>=left-38&&x<=left-28&&y>=mid-18&&y<=mid+18)ci.setReturnValue(false);}
    @Redirect(method="drawGuiContainerBackgroundLayer",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/client/gui/ScreenHelper;drawImageQuad(Lnet/minecraft/util/ResourceLocation;Lcom/mojang/blaze3d/matrix/MatrixStack;FFFFFFFFFFFFF)V"))
    private void backport$flatEdge(ResourceLocation texture,MatrixStack m,float x,float y,float w,float h,float u,float v,float endU,float endV,float r,float g,float b,float a,float z){
        if(w==5&&h==10&&v==172/256f)return; // Native hover arrow.
        if(w==49&&h==166&&u==156/256f&&v==0){
            // Non-overlapping pieces also work with native depth writes enabled.
            // The strip retains the original black outline and two white bevel pixels.
            ScreenHelper.drawImageQuad(texture,m,x+18,y,w-18,h,174/256f,0,endU,endV,r,g,b,a,z);
            ScreenHelper.drawImageQuad(texture,m,x+15,y,3,3,171/256f,0,174/256f,3/256f,r,g,b,a,z);
            ScreenHelper.drawImageQuadUV(texture,m,x+15,y+3,3,h-6,171,8,3,1,r,g,b,a,z);
            ScreenHelper.drawImageQuad(texture,m,x+15,y+h-3,3,3,171/256f,163/256f,174/256f,166/256f,r,g,b,a,z);
        }else ScreenHelper.drawImageQuad(texture,m,x,y,w,h,u,v,endU,endV,r,g,b,a,z);
    }
}
