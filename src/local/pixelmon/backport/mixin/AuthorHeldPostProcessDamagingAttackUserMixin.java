package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.items.HeldItem;
import local.pixelmon.backport.AuthorHeldItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Native effect implementations, including direct getHeldItem call sites. */
@Mixin(targets={"com.pixelmonmod.pixelmon.items.heldItems.RazorFangItem","com.pixelmonmod.pixelmon.items.heldItems.KingsRockItem","com.pixelmonmod.pixelmon.items.heldItems.ShellBellItem"},remap=false)
public abstract class AuthorHeldPostProcessDamagingAttackUserMixin {
    @Inject(method="postProcessDamagingAttackUser",at=@At("HEAD"),cancellable=true,require=1)
    private void backport$gate(PixelmonWrapper a0, PixelmonWrapper a1, Attack a2, float a3, CallbackInfo ci){if(AuthorHeldItems.blocked(a0,(HeldItem)(Object)this,"postProcessDamagingAttackUser" + ":" + a1.getPokemonUUID()))ci.cancel();}
}
