package local.pixelmon.backport.mixin;
import net.minecraft.item.*;
import com.pixelmonmod.pixelmon.items.heldItems.MegaStoneItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Display only; no enchantment tags or evolution rule changes. */
@Mixin(value=Item.class,remap=false)
public abstract class MegaStoneAppearanceMixin {
    @Inject(method="func_77613_e",at=@At("HEAD"),cancellable=true)
    private void backport$purple(ItemStack stack,CallbackInfoReturnable<Rarity> cir){if((Object)this instanceof MegaStoneItem)cir.setReturnValue(Rarity.EPIC);}
}
