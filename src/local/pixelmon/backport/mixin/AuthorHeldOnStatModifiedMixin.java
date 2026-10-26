package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.items.HeldItem;
import local.pixelmon.backport.AuthorHeldItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Native effect implementations, including direct getHeldItem call sites. */
@Mixin(targets={"com.pixelmonmod.pixelmon.items.heldItems.WhiteHerbItem","com.pixelmonmod.pixelmon.items.heldItems.MirrorHerbItem","com.pixelmonmod.pixelmon.items.heldItems.BerryGinemaItem"},remap=false)
public abstract class AuthorHeldOnStatModifiedMixin {
    @Inject(method="onStatModified",at=@At("HEAD"),cancellable=true,require=1)
    private void backport$gate(PixelmonWrapper a0, PixelmonWrapper a1, CallbackInfo ci){if(AuthorHeldItems.blocked(a0,(HeldItem)(Object)this,"onStatModified" + ":" + a1.getPokemonUUID()))ci.cancel();}
}
