package local.pixelmon.backport.mixin;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Initial native alpha/omega placement has no sound; later assembly keeps its own sounds. */
@Mixin(targets="com.pixelmonmod.pixelmon.items.ShardItem",remap=false)
public abstract class ShardPlacementSoundMixin {
 @Redirect(method="func_195939_a",at=@At(value="INVOKE",target="Lnet/minecraft/world/World;func_175656_a(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
 private boolean backport$firstPiece(World world,BlockPos pos,BlockState state){
  boolean placed=world.func_175656_a(pos,state);
  if(placed&&!world.field_72995_K&&state.func_177230_c() instanceof com.pixelmonmod.pixelmon.blocks.OrbBlock)local.pixelmon.backport.DeltaOrb.placementSound(world,pos);
  return placed;
 }
}
