package local.pixelmon.backport.mixin;
import local.pixelmon.backport.EndlessHeldItems;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemStack.class)
public abstract class EndlessItemStackMixin {
 @Inject(method="func_77962_s",at=@At("HEAD"),cancellable=true)
 private void backport$glint(CallbackInfoReturnable<Boolean> ci){if(EndlessHeldItems.is((ItemStack)(Object)this))ci.setReturnValue(true);}
 @Inject(method="func_200301_q",at=@At("RETURN"),cancellable=true)
 private void backport$name(CallbackInfoReturnable<ITextComponent> ci){if(EndlessHeldItems.is((ItemStack)(Object)this))ci.setReturnValue(new TranslationTextComponent("pixelmon.backport.endless.name",ci.getReturnValue()));}
}
