package local.pixelmon.backport.mixin;
import net.minecraft.inventory.container.*;
import net.minecraft.item.*;
import net.minecraft.enchantment.EnchantmentHelper;
import local.pixelmon.backport.FruitCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Forge cancellation leaves a previous rename result; invalidate it for prohibited inputs. */
@Mixin(RepairContainer.class)
public abstract class FruitCollectorAnvilMixin {
 @Inject(method="func_82848_d",at=@At("RETURN"))
 private void backport$clearRejected(CallbackInfo ci){var c=(RepairContainer)(Object)this;var left=c.func_75139_a(0).func_75211_c();var right=c.func_75139_a(1).func_75211_c();if(FruitCollector.is(left)&&(right.func_77973_b()==Items.field_151045_i&&left.func_77952_i()==0||EnchantmentHelper.func_82781_a(right).keySet().stream().anyMatch(FruitCollector::forbidden))){c.func_75139_a(2).func_75215_d(ItemStack.field_190927_a);c.setMaximumCost(0);c.field_82856_l=0;}}
}
