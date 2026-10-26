package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.items.HammerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.block.BlockState;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** The invisible ore companion must inherit the native ore's exemption from hammer area-smashing. */
@Mixin(value=HammerItem.class,remap=false)
public abstract class TumblestoneHammerMixin {
 @Inject(method="func_179218_a",at=@At("HEAD"),cancellable=true)
 private void backport$upperOre(ItemStack stack,World world,BlockState state,BlockPos pos,LivingEntity user,CallbackInfoReturnable<Boolean> ci){if(world.func_180495_p(pos).func_177230_c() instanceof local.pixelmon.backport.TumblestoneFootprint.Upper)ci.setReturnValue(false);}
}
