package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.drops.ItemWithChance;
import local.pixelmon.backport.PokeChestLoot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=ItemWithChance.class,remap=false)
public abstract class RewardChanceItemMixin {
    @Shadow @Final private ItemStack itemStack;
    @Inject(method={"getItem","getItemStack"},at=@At("HEAD"),cancellable=true)
    private void backport$invalidItem(CallbackInfoReturnable<ItemStack> ci){if(!PokeChestLoot.valid(itemStack))ci.setReturnValue(ItemStack.field_190927_a);}
}
