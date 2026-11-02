package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.EndlessHeldItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets="com.pixelmonmod.pixelmon.items.heldItems.AirBalloonItem",remap=false)
public abstract class EndlessBalloonMixin {
 @Redirect(method="postProcessDamagingAttackTarget",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;removeHeldItem()V"),require=1)
 private void backport$retain(PixelmonWrapper p){if(!EndlessHeldItems.trigger(p))p.removeHeldItem();}
}
