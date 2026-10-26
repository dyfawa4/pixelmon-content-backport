package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.items.HeldItem;
import local.pixelmon.backport.AuthorHeldItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Native effect implementations, including direct getHeldItem call sites. */
@Mixin(targets={"com.pixelmonmod.pixelmon.items.heldItems.BerryStatResponseItem","com.pixelmonmod.pixelmon.items.heldItems.AirBalloonItem","com.pixelmonmod.pixelmon.items.heldItems.RaiseStatItem","com.pixelmonmod.pixelmon.items.heldItems.RedCardItem","com.pixelmonmod.pixelmon.items.heldItems.WeaknessPolicyItem","com.pixelmonmod.pixelmon.items.heldItems.EjectButtonItem","com.pixelmonmod.pixelmon.items.heldItems.BerryRecoilItem"},remap=false)
public abstract class AuthorHeldPostProcessDamagingAttackTargetMixin {
    @Inject(method="postProcessDamagingAttackTarget",at=@At("HEAD"),cancellable=true,require=1)
    private void backport$gate(PixelmonWrapper a0, PixelmonWrapper a1, Attack a2, float a3, CallbackInfo ci){if(AuthorHeldItems.blocked(a1,(HeldItem)(Object)this,"postProcessDamagingAttackTarget" + ":" + a0.getPokemonUUID()))ci.cancel();}
}
