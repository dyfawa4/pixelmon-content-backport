package qa.reviewmixin;
import qa.Review110Probe;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Private fixture: exercises native expanded tooltips without foreground keyboard input. */
@Mixin(value=ScreenHelper.class,remap=false)
public abstract class Review110ShiftMixin {
 @Inject(method="isKeyDown",at=@At("HEAD"),cancellable=true)
 private static void expanded(int key,CallbackInfoReturnable<Boolean> ci){if(key==340&&Review110Probe.shift)ci.setReturnValue(true);}
}
