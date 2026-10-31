package local.pixelmon.backport.mixin;
import local.pixelmon.backport.FishingEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Anvil applicability is separate from enchanting-table eligibility in Forge. */
@Mixin(value=Enchantment.class,remap=false)
public abstract class FishingBookEnchantmentMixin {
    @Inject(method="func_92089_a",at=@At("HEAD"),cancellable=true)
    private void backport$rodBook(ItemStack stack,CallbackInfoReturnable<Boolean> ci){
        if(local.pixelmon.backport.FruitCollector.is(stack)){var enchantment=(Enchantment)(Object)this;if(local.pixelmon.backport.FruitCollector.forbidden(enchantment))ci.setReturnValue(false);else if(enchantment==net.minecraft.enchantment.Enchantments.field_185308_t)ci.setReturnValue(true);}
        if(FishingEnchantments.supported(stack)&&FishingEnchantments.bookOnly((Enchantment)(Object)this))ci.setReturnValue(true);
    }
}
