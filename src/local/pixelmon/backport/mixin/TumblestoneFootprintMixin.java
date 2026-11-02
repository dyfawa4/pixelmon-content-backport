package local.pixelmon.backport.mixin;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=World.class,remap=false)
public abstract class TumblestoneFootprintMixin {
 @Inject(method="func_241211_a_",at=@At("HEAD"),cancellable=true)
 private void backport$noSnow(BlockPos pos,BlockState state,int flags,int recursion,CallbackInfoReturnable<Boolean> ci){if(local.pixelmon.backport.TumblestoneFootprint.snowBlocked((World)(Object)this,pos,state))ci.setReturnValue(false);}
 @Inject(method="func_241211_a_",at=@At("RETURN"))
 private void backport$oreState(BlockPos pos,BlockState state,int flags,int recursion,CallbackInfoReturnable<Boolean> ci){if(ci.getReturnValue()||local.pixelmon.backport.TumblestoneFootprint.ore(state)){
  World world=(World)(Object)this;local.pixelmon.backport.TumblestoneFootprint.changed(world,pos);
  if(local.pixelmon.backport.TumblestoneFootprint.ore(world.func_180495_p(pos.func_177977_b())))local.pixelmon.backport.TumblestoneFootprint.changed(world,pos.func_177977_b());
 }}
}
