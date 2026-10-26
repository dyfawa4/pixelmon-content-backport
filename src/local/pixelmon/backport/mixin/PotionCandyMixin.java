package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.pixelmon.interactions.InteractionRareCandy;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import local.pixelmon.backport.MysteriousPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=InteractionRareCandy.class,remap=false)
public abstract class PotionCandyMixin {
    @Inject(method="processInteract",at=@At("HEAD"),cancellable=true)
    private void backport$restore(PixelmonEntity entity,PlayerEntity player,Hand hand,ItemStack stack,CallbackInfoReturnable<Boolean> cir){Boolean result=MysteriousPotion.candy(entity,player,hand,stack);if(result!=null)cir.setReturnValue(result);}
}
