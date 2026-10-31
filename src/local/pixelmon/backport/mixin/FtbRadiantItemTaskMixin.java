package local.pixelmon.backport.mixin;
import net.minecraft.item.ItemStack;
import com.pixelmonmod.pixelmon.items.PokeBallItem;
import local.pixelmon.backport.RadiantBallLogic;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** The shared ball registry item does not identify a specific ball without NBT. */
@Pseudo @Mixin(targets="dev.ftb.mods.ftbquests.quest.task.ItemTask",remap=false)
public abstract class FtbRadiantItemTaskMixin {
 @Shadow public ItemStack item;
 @Inject(method="test(Lnet/minecraft/item/ItemStack;)Z",at=@At("RETURN"),cancellable=true)
 private void backport$exactRadiant(ItemStack offered,CallbackInfoReturnable<Boolean> cir){
  if(!cir.getReturnValueZ()||item==null||!(item.func_77973_b() instanceof PokeBallItem)||!item.func_77942_o()
   ||!RadiantBallLogic.NAME.equals(item.func_77978_p().func_74779_i("PokeBallID")))return;
  if(offered==null||offered.func_190926_b()||!offered.func_77942_o()||!RadiantBallLogic.NAME.equals(offered.func_77978_p().func_74779_i("PokeBallID")))cir.setReturnValue(false);
 }
}
