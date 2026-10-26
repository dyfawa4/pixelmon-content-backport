package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.items.HeldItem;
import local.pixelmon.backport.AuthorHeldItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Native effect implementations, including direct getHeldItem call sites. */
@Mixin(targets={"com.pixelmonmod.pixelmon.items.heldItems.RingTargetItem","com.pixelmonmod.pixelmon.items.heldItems.IronBallItem"},remap=false)
public abstract class AuthorHeldGetEffectiveTypesMixin {
    @Inject(method="getEffectiveTypes",at=@At("HEAD"),cancellable=true,require=1)
    private void backport$gate(PixelmonWrapper a0, PixelmonWrapper a1, CallbackInfoReturnable<java.util.List<com.pixelmonmod.pixelmon.api.pokemon.Element>> ci){if(AuthorHeldItems.blocked(a1,(HeldItem)(Object)this,"getEffectiveTypes" + ":" + a0.getPokemonUUID()))ci.setReturnValue(null);}
}
