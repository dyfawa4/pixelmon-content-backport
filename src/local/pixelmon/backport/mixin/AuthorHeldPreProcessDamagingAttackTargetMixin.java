package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.items.HeldItem;
import local.pixelmon.backport.AuthorHeldItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Native effect implementations, including direct getHeldItem call sites. */
@Mixin(targets={"com.pixelmonmod.pixelmon.items.heldItems.BerryTypeReducingItem"},remap=false)
public abstract class AuthorHeldPreProcessDamagingAttackTargetMixin {
    @Inject(method="preProcessDamagingAttackTarget",at=@At("HEAD"),cancellable=true,require=1)
    private void backport$gate(PixelmonWrapper a0, PixelmonWrapper a1, Attack a2, double a3, CallbackInfoReturnable<Double> ci){if(AuthorHeldItems.blocked(a1,(HeldItem)(Object)this,"preProcessDamagingAttackTarget" + ":" + a0.getPokemonUUID()))ci.setReturnValue(a3);}
}
