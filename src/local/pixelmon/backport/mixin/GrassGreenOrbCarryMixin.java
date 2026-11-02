package local.pixelmon.backport.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.item.ItemStack;import net.minecraft.util.Hand;import net.minecraft.entity.player.*;import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
@Mixin(targets="com.pixelmonmod.pixelmon.entities.pixelmon.interactions.InteractionHeldItem",remap=false)
public abstract class GrassGreenOrbCarryMixin {
 @Inject(method="processInteract",at=@At("HEAD"),cancellable=true)
 private void backport$carry(PixelmonEntity entity,PlayerEntity player,Hand hand,ItemStack stack,CallbackInfoReturnable<Boolean> ci){
  if(!(player instanceof ServerPlayerEntity)||entity.getOwner()!=player||hand!=Hand.MAIN_HAND||stack.func_77973_b()!=com.pixelmonmod.pixelmon.api.registries.PixelmonItems.jade_orb)return;
  var previous=entity.getPokemon().getHeldItem();if(!previous.func_190926_b()&&previous.func_77973_b()==stack.func_77973_b()){ci.setReturnValue(true);return;}
  if(!previous.func_190926_b())entity.func_70099_a(previous.func_77946_l(),1);
  var given=stack.func_77946_l();given.func_190920_e(1);entity.getPokemon().setHeldItem(given);stack.func_190918_g(1);
  entity.update(new com.pixelmonmod.pixelmon.comm.EnumUpdateType[]{com.pixelmonmod.pixelmon.comm.EnumUpdateType.HeldItem});ci.setReturnValue(true);
 }
}
