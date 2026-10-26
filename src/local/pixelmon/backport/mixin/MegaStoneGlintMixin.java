package local.pixelmon.backport.mixin;
import net.minecraft.item.ItemStack;
import com.pixelmonmod.pixelmon.items.PixelmonItem;
import com.pixelmonmod.pixelmon.items.heldItems.MegaStoneItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** PixelmonItem overrides Item's glint predicate. */
@Mixin(value=PixelmonItem.class,remap=false)
public abstract class MegaStoneGlintMixin {
    @Inject(method="func_77636_d",at=@At("HEAD"),cancellable=true)
    private void backport$glint(ItemStack stack,CallbackInfoReturnable<Boolean> cir){if((Object)this instanceof MegaStoneItem)cir.setReturnValue(true);}
}
