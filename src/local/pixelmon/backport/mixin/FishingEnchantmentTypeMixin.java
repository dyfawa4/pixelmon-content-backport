package local.pixelmon.backport.mixin;
import net.minecraft.item.Item;
import com.pixelmonmod.pixelmon.items.FishingRodItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** The verified 1.16.5 FISHING_ROD enum implementation; all enchanting/anvil callers share this predicate. */
@Mixin(targets="net.minecraft.enchantment.EnchantmentType$8",remap=false)
public abstract class FishingEnchantmentTypeMixin {
    @Inject(method="func_77557_a",at=@At("HEAD"),cancellable=true)
    private void backport$pixelmonRod(Item item,CallbackInfoReturnable<Boolean> ci){if(item instanceof FishingRodItem)ci.setReturnValue(true);}
}
