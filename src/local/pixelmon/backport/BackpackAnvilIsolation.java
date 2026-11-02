package local.pixelmon.backport;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
/** An anvil rename may only inherit the input backpack's contents reference. */
public final class BackpackAnvilIsolation {
 public static boolean backpack(ItemStack s){var id=s.func_77973_b().getRegistryName();return !s.func_190926_b()&&id!=null&&id.func_110624_b().equals("sophisticatedbackpacks")&&(id.func_110623_a().equals("backpack")||id.func_110623_a().endsWith("_backpack"));}
 public static ItemStack renamed(ItemStack input,ItemStack output){
  var fresh=output.func_77955_b(new CompoundNBT());var tag=fresh.func_74775_l("tag");
  if(input.func_77942_o()&&input.func_77978_p().func_186855_b("contentsUuid"))tag.func_218657_a("contentsUuid",input.func_77978_p().func_74781_a("contentsUuid").func_74737_b());
  else tag.func_82580_o("contentsUuid");
  fresh.func_218657_a("tag",tag);
  // Fresh capability provider: no cached wrapper from a previous output stack.
  return ItemStack.func_199557_a(fresh);
 }
}
