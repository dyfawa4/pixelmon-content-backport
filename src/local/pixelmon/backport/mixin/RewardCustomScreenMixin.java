package local.pixelmon.backport.mixin;
import java.util.List;
import com.pixelmonmod.pixelmon.api.drops.CustomDropScreenFactory;
import local.pixelmon.backport.RewardSanitizer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=CustomDropScreenFactory.Builder.class,remap=false)
public abstract class RewardCustomScreenMixin {
    @Shadow protected List<ItemStack> items;
    @Inject(method="sendTo",at=@At("HEAD"))
    private void backport$sharedItems(ServerPlayerEntity player,CallbackInfoReturnable<String> ci){items=RewardSanitizer.stacks(items);}
}
