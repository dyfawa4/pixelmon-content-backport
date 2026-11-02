package local.pixelmon.backport.mixin;
import net.minecraft.block.*;
import net.minecraft.world.IWorldReader;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Native biome snowfall/world-generation checks use SnowBlock.isValidPosition before placing. */
@Mixin(value=SnowBlock.class,remap=false)
public abstract class TumblestoneSnowMixin {
 @Inject(method="func_196260_a",at=@At("HEAD"),cancellable=true)
 private void backport$noSnow(BlockState s,IWorldReader world,BlockPos pos,CallbackInfoReturnable<Boolean> ci){var below=world.func_180495_p(pos.func_177977_b());if(local.pixelmon.backport.TumblestoneFootprint.ore(below)||below.func_177230_c() instanceof local.pixelmon.backport.TumblestoneFootprint.Upper)ci.setReturnValue(false);}
}
