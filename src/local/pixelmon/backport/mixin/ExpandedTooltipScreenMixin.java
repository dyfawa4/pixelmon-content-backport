package local.pixelmon.backport.mixin;
import local.pixelmon.backport.client.ExpandedItemTooltips;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Screen.class)
public abstract class ExpandedTooltipScreenMixin {
 @Inject(method="func_231173_s_",at=@At("HEAD"),cancellable=true)
 private static void backport$tooltipShift(CallbackInfoReturnable<Boolean> ci){if(ExpandedItemTooltips.active())ci.setReturnValue(true);}
}
