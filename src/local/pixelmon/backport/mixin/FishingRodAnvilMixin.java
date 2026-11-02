package local.pixelmon.backport.mixin;
import net.minecraft.inventory.container.RepairContainer;
import net.minecraft.item.ItemStack;
import local.pixelmon.backport.FishingEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Enforce rod book eligibility even in creative mode and invalidate stale rename output. */
@Mixin(RepairContainer.class)
public abstract class FishingRodAnvilMixin {
 @Inject(method="func_82848_d",at=@At("RETURN"))
 private void backport$rejectBook(CallbackInfo ci){
  var c=(RepairContainer)(Object)this;
  if(FishingEnchantments.rejectsBook(c.func_75139_a(0).func_75211_c(),c.func_75139_a(1).func_75211_c())){
   c.func_75139_a(2).func_75215_d(ItemStack.field_190927_a);c.setMaximumCost(0);c.field_82856_l=0;
  }
 }
}
