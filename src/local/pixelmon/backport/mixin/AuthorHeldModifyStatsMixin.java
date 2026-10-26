package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.items.HeldItem;
import local.pixelmon.backport.AuthorHeldItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Native effect implementations, including direct getHeldItem call sites. */
@Mixin(targets={"com.pixelmonmod.pixelmon.items.heldItems.DeepSeaToothScaleItem","com.pixelmonmod.pixelmon.items.heldItems.StatEnhancingItems","com.pixelmonmod.pixelmon.items.heldItems.EVAdjusting","com.pixelmonmod.pixelmon.items.heldItems.IronBallItem","com.pixelmonmod.pixelmon.items.heldItems.MuscleBandItem","com.pixelmonmod.pixelmon.items.heldItems.AssaultVestItem","com.pixelmonmod.pixelmon.items.heldItems.ChoiceItem"},remap=false)
public abstract class AuthorHeldModifyStatsMixin {
    @Inject(method="modifyStats",at=@At("HEAD"),cancellable=true,require=1)
    private void backport$gate(PixelmonWrapper a0, int[] a1, CallbackInfoReturnable<int[]> ci){if(AuthorHeldItems.blocked(a0,(HeldItem)(Object)this,"modifyStats"))ci.setReturnValue(a1);}
}
