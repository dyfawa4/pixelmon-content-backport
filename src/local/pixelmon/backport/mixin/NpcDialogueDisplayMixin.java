package local.pixelmon.backport.mixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import net.minecraft.client.gui.screen.Screen;
import local.pixelmon.backport.client.NpcDialogueDisplay;
import local.pixelmon.backport.client.NpcDialogueLayout;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=ScreenHelper.class,remap=false)
public abstract class NpcDialogueDisplayMixin {
    @Redirect(method="drawDialogueBox(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/gui/screen/Screen;Ljava/lang/String;Ljava/lang/String;F)V",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/client/gui/ScreenHelper;drawCenteredSplitString(Lcom/mojang/blaze3d/matrix/MatrixStack;Ljava/lang/String;FFIIZ)V"),require=1,allow=1)
    private static void backport$wrapDialogue(MatrixStack matrix,String text,float x,float y,int width,int color,boolean shadow,MatrixStack originalMatrix,Screen screen,String title,String body,float extra){
        if(NpcDialogueDisplay.applies(screen))NpcDialogueLayout.draw(matrix,text,x,y,width,color,shadow);
        else ScreenHelper.drawCenteredSplitString(matrix,text,x,y,width,color,shadow);
    }
    @ModifyVariable(method="drawDialogueBox(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/gui/screen/Screen;Ljava/lang/String;Ljava/util/List;F)V",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static String backport$localizedTitle(String value,MatrixStack matrix,Screen screen,String title,List<String> lines,float extra){return NpcDialogueDisplay.applies(screen)?NpcDialogueDisplay.text(value):value;}
    @ModifyVariable(method="drawDialogueBox(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/gui/screen/Screen;Ljava/lang/String;Ljava/util/List;F)V",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static List<String> backport$localizedPages(List<String> value,MatrixStack matrix,Screen screen,String title,List<String> lines,float extra){return NpcDialogueDisplay.applies(screen)?NpcDialogueDisplay.pages(value):value;}
}
