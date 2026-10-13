package local.pixelmon.backport.mixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets="com.pixelmonmod.pixelmon.client.gui.npc.ChattingNPCScreen",remap=false)
public abstract class NpcDialogueDisplayMixin {
    @Redirect(method="func_230430_a_",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/client/gui/ScreenHelper;drawDialogueBox(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/gui/screen/Screen;Ljava/lang/String;Ljava/util/List;F)V"))
    private void backport$localizedTitle(MatrixStack matrix,Screen screen,String title,List<String> lines,float extra){
        ScreenHelper.drawDialogueBox(matrix,screen,I18n.func_135052_a(title),lines,extra);
    }
}
