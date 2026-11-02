package local.pixelmon.backport.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Native wild-only first-orb callback must not double-count our knockout listener. */
@Mixin(targets="com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant",remap=false)
public abstract class ShrineOrbChargeMixin {
 @Inject(method="checkPlayerItems",at=@At("HEAD"),cancellable=true)
 private void backport$chargeAtKnockout(CallbackInfo ci){ci.cancel();}
}
