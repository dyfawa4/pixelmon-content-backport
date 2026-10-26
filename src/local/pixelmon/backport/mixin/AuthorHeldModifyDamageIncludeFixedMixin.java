package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.items.HeldItem;
import local.pixelmon.backport.AuthorHeldItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Native effect implementations, including direct getHeldItem call sites. */
@Mixin(targets={"com.pixelmonmod.pixelmon.items.heldItems.FocusBandItem","com.pixelmonmod.pixelmon.items.heldItems.FocusSashItem"},remap=false)
public abstract class AuthorHeldModifyDamageIncludeFixedMixin {
    @Inject(method="modifyDamageIncludeFixed",at=@At("HEAD"),cancellable=true,require=1)
    private void backport$gate(double a0, PixelmonWrapper a1, PixelmonWrapper a2, Attack a3, CallbackInfoReturnable<Double> ci){if(AuthorHeldItems.blocked(a2,(HeldItem)(Object)this,"modifyDamageIncludeFixed" + ":" + a1.getPokemonUUID()))ci.setReturnValue(a0);}
}
