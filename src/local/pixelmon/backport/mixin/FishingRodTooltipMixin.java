package local.pixelmon.backport.mixin;
import local.pixelmon.backport.FishingEnchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.text.*;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Client tooltip descriptor must never be transformed on a dedicated server. */
@Mixin(targets="com.pixelmonmod.pixelmon.items.FishingRodItem",remap=false)
public abstract class FishingRodTooltipMixin {
    @Inject(method="func_77624_a",at=@At("TAIL"))
    private void backport$fishingRules(ItemStack stack,World world,List<ITextComponent> tooltip,ITooltipFlag flag,CallbackInfo ci){
        if(!FishingEnchantments.supported(stack))return;
        tooltip.add(new TranslationTextComponent("tooltip.pixelmon_backport.fishing.books"));
        int total=FishingEnchantments.total(stack);
        if(total==0)tooltip.add(new TranslationTextComponent("tooltip.pixelmon_backport.fishing.zero"));
        else{
            tooltip.add(new TranslationTextComponent("tooltip.pixelmon_backport.fishing.bonus",total,total*10));
            tooltip.add(new TranslationTextComponent("tooltip.pixelmon_backport.fishing.cost",FishingEnchantments.catchDamage(stack)));
        }
    }
}
