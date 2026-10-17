package local.pixelmon.backport.mixin;
import java.util.ArrayList;
import com.pixelmonmod.pixelmon.battles.raids.WeightedItemStacks;
import local.pixelmon.backport.PokeChestLoot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=WeightedItemStacks.class,remap=false)
public abstract class RewardWeightedItemsMixin {
    @Shadow private int total;
    @Shadow @Final private ArrayList<Tuple<Integer,ItemStack>> stacks;
    @Inject(method="add",at=@At("HEAD"),cancellable=true)
    private void backport$validEntry(int weight,ItemStack stack,CallbackInfoReturnable<WeightedItemStacks> ci){if(weight<=0||!PokeChestLoot.valid(stack))ci.setReturnValue((WeightedItemStacks)(Object)this);}
    @Inject(method={"get","pop"},at=@At("HEAD"))
    private void backport$validPool(CallbackInfoReturnable<ItemStack> ci){
        for(var it=stacks.iterator();it.hasNext();){var entry=it.next();if(!PokeChestLoot.valid(entry.func_76340_b())||entry.func_76341_a()<=0){total-=entry.func_76341_a();it.remove();}}
    }
}
