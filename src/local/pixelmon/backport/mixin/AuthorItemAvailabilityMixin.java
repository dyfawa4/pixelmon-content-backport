package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.items.HeldItem;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.AuthorHeldItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=HeldItem.class,remap=false)
public abstract class AuthorItemAvailabilityMixin {
    @Inject(method="canUseItem",at=@At("HEAD"),cancellable=true)
    private static void backport$initialTurns(PixelmonWrapper owner,CallbackInfoReturnable<Boolean> ci){
        HeldItem item=owner.getHeldItem();
        if(AuthorHeldItems.suppressed(owner,item)||item!=null&&item.getRegistryName()!=null&&(item.getRegistryName().func_110624_b().equals("pixelmon_backport")||item instanceof com.pixelmonmod.pixelmon.items.heldItems.ProtectivePadsItem)&&AuthorHeldItems.blocked(owner,item,"custom-effect"))ci.setReturnValue(false);
    }
}
