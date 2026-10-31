package local.pixelmon.backport.mixin;
import net.minecraft.item.ItemStack;
import com.pixelmonmod.pixelmon.items.PixelmonItem;
import com.pixelmonmod.pixelmon.items.heldItems.MegaStoneItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** PixelmonItem overrides Item's glint predicate; balls share one registry item. */
@Mixin(value=PixelmonItem.class,remap=false)
public abstract class MegaStoneGlintMixin {
    @Inject(method="func_77636_d",at=@At("HEAD"),cancellable=true)
    private void backport$glint(ItemStack stack,CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof MegaStoneItem)cir.setReturnValue(true);
        else if((Object)this instanceof com.pixelmonmod.pixelmon.items.PokeBallItem
            &&com.pixelmonmod.pixelmon.items.PokeBallPart.getPokeBall(stack).map(ball->local.pixelmon.backport.RadiantBallLogic.NAME.equals(ball.getName())).orElse(false))cir.setReturnValue(true);
    }
}
