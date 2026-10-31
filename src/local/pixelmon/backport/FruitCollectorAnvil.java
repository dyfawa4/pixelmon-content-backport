package local.pixelmon.backport;
import net.minecraft.item.*;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=Content.NS)
public final class FruitCollectorAnvil {
 @SubscribeEvent(priority=EventPriority.HIGH) public static void update(AnvilUpdateEvent e){
  if(!FruitCollector.is(e.getLeft()))return;
  if(EnchantmentHelper.func_82781_a(e.getRight()).keySet().stream().anyMatch(FruitCollector::forbidden)){e.setCanceled(true);return;}
  if(e.getRight().func_77973_b()!=Items.field_151045_i)return;
  int damage=e.getLeft().func_77952_i();if(damage<=0){e.setCanceled(true);return;}
  int used=Math.min(e.getRight().func_190916_E(),(damage+2)/3);var result=e.getLeft().func_77946_l();result.func_196085_b(Math.max(0,damage-3*used));int cost=used+e.getLeft().func_82838_A();
  String name=e.getName();if(name!=null){if(name.isBlank()){if(result.func_82837_s()){result.func_135074_t();cost++;}}else if(!name.equals(result.func_200301_q().getString())){result.func_200302_a(new net.minecraft.util.text.StringTextComponent(name));cost++;}}
  result.func_82841_c(e.getLeft().func_82838_A());e.setOutput(result);e.setMaterialCost(used);e.setCost(Math.max(1,cost));
 }
}
