package local.pixelmon.backport.mixin;
import local.pixelmon.backport.DisposableTeraDisc;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.interactions.InteractionTechnicalMove;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=InteractionTechnicalMove.class,remap=false)
public abstract class DisposableDiscInteractionMixin {
    @Inject(method="processInteract",at=@At("HEAD"),cancellable=true)
    private void backport$disc(PixelmonEntity pokemon,PlayerEntity player,Hand hand,ItemStack stack,CallbackInfoReturnable<Boolean> ci){
        if(stack.func_77973_b() instanceof DisposableTeraDisc)ci.setReturnValue(DisposableTeraDisc.interact(pokemon,player,hand,stack));
    }
}
