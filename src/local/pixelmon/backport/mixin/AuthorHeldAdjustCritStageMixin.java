package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.items.HeldItem;
import local.pixelmon.backport.AuthorHeldItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Native effect implementations, including direct getHeldItem call sites. */
@Mixin(targets={"com.pixelmonmod.pixelmon.items.heldItems.CritEnhancingItem","com.pixelmonmod.pixelmon.items.heldItems.ScopeLensItem","com.pixelmonmod.pixelmon.items.heldItems.RazorClawItem"},remap=false)
public abstract class AuthorHeldAdjustCritStageMixin {
    @Inject(method="adjustCritStage",at=@At("HEAD"),cancellable=true,require=1)
    private void backport$gate(PixelmonWrapper a0, CallbackInfoReturnable<Integer> ci){if(AuthorHeldItems.blocked(a0,(HeldItem)(Object)this,"adjustCritStage"))ci.setReturnValue(0);}
}
