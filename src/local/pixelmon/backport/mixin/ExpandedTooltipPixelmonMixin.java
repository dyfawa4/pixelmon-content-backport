package local.pixelmon.backport.mixin;
import local.pixelmon.backport.client.ExpandedItemTooltips;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=ScreenHelper.class,remap=false)
public abstract class ExpandedTooltipPixelmonMixin {
 @Inject(method="isKeyDown",at=@At("HEAD"),cancellable=true)
 private static void backport$tooltipShift(int key,CallbackInfoReturnable<Boolean> ci){if((key==340||key==344)&&ExpandedItemTooltips.active())ci.setReturnValue(true);}
}
