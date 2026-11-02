package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.EndlessHeldItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Retain the physical item, but exhaust its effect for this battle. */
@Mixin(value=PixelmonWrapper.class,remap=false)
public abstract class EndlessHeldConsumeMixin {
 @Inject(method="consumeItem",at=@At("HEAD"),cancellable=true)
 private void backport$independent(CallbackInfo ci){if(EndlessHeldItems.trigger((PixelmonWrapper)(Object)this))ci.cancel();}
 @Inject(method="getUsableHeldItem",at=@At("HEAD"),cancellable=true)
 private void backport$once(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<com.pixelmonmod.pixelmon.items.HeldItem> ci){if(EndlessHeldItems.spent((PixelmonWrapper)(Object)this))ci.setReturnValue(com.pixelmonmod.pixelmon.items.heldItems.NoItem.noItem);}
}
