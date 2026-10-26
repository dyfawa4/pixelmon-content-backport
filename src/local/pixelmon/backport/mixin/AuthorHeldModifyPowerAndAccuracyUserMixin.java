package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.items.HeldItem;
import local.pixelmon.backport.AuthorHeldItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Native effect implementations, including direct getHeldItem call sites. */
@Mixin(targets={"com.pixelmonmod.pixelmon.items.heldItems.ZoomLensItem","com.pixelmonmod.pixelmon.items.heldItems.PunchingGloveItem","com.pixelmonmod.pixelmon.items.heldItems.WideLensItem","com.pixelmonmod.pixelmon.items.heldItems.BerryMicleItem"},remap=false)
public abstract class AuthorHeldModifyPowerAndAccuracyUserMixin {
    @Inject(method="modifyPowerAndAccuracyUser",at=@At("HEAD"),cancellable=true,require=1)
    private void backport$gate(int[] a0, PixelmonWrapper a1, PixelmonWrapper a2, Attack a3, CallbackInfoReturnable<int[]> ci){if(AuthorHeldItems.blocked(a1,(HeldItem)(Object)this,"modifyPowerAndAccuracyUser" + ":" + a2.getPokemonUUID()))ci.setReturnValue(a0);}
}
