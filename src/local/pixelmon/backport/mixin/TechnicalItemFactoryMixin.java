package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import com.pixelmonmod.pixelmon.enums.TMType;
import com.pixelmonmod.pixelmon.enums.technicalmoves.ITechnicalMove;
import com.pixelmonmod.pixelmon.items.TechnicalMoveItem;
import local.pixelmon.backport.BackportTechnicalMoves;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=PixelmonItems.class,remap=false)
public abstract class TechnicalItemFactoryMixin {
    @Inject(method="createTMStackFor(Lcom/pixelmonmod/pixelmon/enums/technicalmoves/ITechnicalMove;I)Lnet/minecraft/item/ItemStack;",at=@At("HEAD"),cancellable=true)
    private static void backport$stack(ITechnicalMove move,int count,CallbackInfoReturnable<ItemStack> ci){
        if(move instanceof BackportTechnicalMoves){ItemStack stack=TechnicalMoveItem.of(TMType.TM9,move);stack.func_190920_e(count);ci.setReturnValue(stack);}
    }
}
