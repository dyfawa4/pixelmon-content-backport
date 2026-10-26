package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.items.PixelmonItem;
import com.pixelmonmod.pixelmon.items.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.IVanishable;
import org.spongepowered.asm.mixin.Mixin;
/** Same enchanting value/vanishing eligibility as the vanilla rod; durability/mending/grindstone stay native. */
@Mixin(value=FishingRodItem.class,remap=false)
public abstract class FishingRodEnchantabilityMixin extends PixelmonItem implements IVanishable {
    protected FishingRodEnchantabilityMixin(Item.Properties properties){super(properties);}
    @Override public int func_77619_b(){return 1;}
    @Override public boolean func_77616_k(ItemStack stack){return stack.func_77976_d()==1&&stack.func_77984_f();}
    @Override public boolean isBookEnchantable(ItemStack stack,ItemStack book){return true;}
}
