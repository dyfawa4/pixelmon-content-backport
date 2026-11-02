package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.items.PixelmonItem;
import com.pixelmonmod.pixelmon.items.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.IVanishable;
import net.minecraft.enchantment.Enchantment;
import local.pixelmon.backport.FishingEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.pixelmonmod.pixelmon.entities.projectiles.HookEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.util.text.*;
import java.util.List;
/** Same enchanting value/vanishing eligibility as the vanilla rod; durability/mending/grindstone stay native. */
@Mixin(value=FishingRodItem.class,remap=false)
public abstract class FishingRodEnchantabilityMixin extends PixelmonItem implements IVanishable {
    protected FishingRodEnchantabilityMixin(Item.Properties properties){super(properties);}
    @Override public int func_77619_b(){return 1;}
    @Override public boolean func_77616_k(ItemStack stack){return stack.func_77976_d()==1&&stack.func_77984_f();}
    @Override public boolean isBookEnchantable(ItemStack stack,ItemStack book){return !FishingEnchantments.rejectsBook(stack,book);}
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack,Enchantment enchantment){
        return FishingEnchantments.supported(stack)?FishingEnchantments.allowed(enchantment)&&!FishingEnchantments.bookOnly(enchantment):super.canApplyAtEnchantingTable(stack,enchantment);
    }
    @Inject(method="func_77659_a",at=@At("RETURN"))
    private void backport$castingHand(World world,PlayerEntity player,Hand hand,CallbackInfoReturnable<ActionResult<ItemStack>> ci){
        if(!world.field_72995_K&&player.field_71104_cf instanceof HookEntity)FishingEnchantments.bind((HookEntity)player.field_71104_cf,player.func_184586_b(hand));
    }
}
