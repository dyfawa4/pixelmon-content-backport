package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.items.PixelmonItem;
import com.pixelmonmod.pixelmon.items.heldItems.MegaStoneItem;
import local.pixelmon.backport.TeraDenItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The unified Shift catalogue replaces the native description, not appends to it. */
@Mixin(value=PixelmonItem.class,remap=false)
public abstract class MegaStoneTooltipMixin {
    @Inject(method="func_77624_a",at=@At("HEAD"),cancellable=true)
    private void backport$oneDescription(CallbackInfo ci){if((Object)this instanceof MegaStoneItem||(Object)this instanceof TeraDenItem)ci.cancel();}
}
