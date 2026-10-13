package local.pixelmon.backport.mixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.screens.bag.ItemMenuButton",remap=false)
public abstract class BurstBagDisplayMixin {
    @Shadow public abstract ItemStack getStack();
    @Inject(method="func_230431_b_",at=@At("RETURN"))
    private void backport$burstBorder(MatrixStack matrix,int mouseX,int mouseY,float partial,CallbackInfo ci){if(local.pixelmon.backport.BurstClient.ball(getStack()))local.pixelmon.backport.BurstClient.outline(matrix,(net.minecraft.client.gui.widget.Widget)(Object)this);}
}
