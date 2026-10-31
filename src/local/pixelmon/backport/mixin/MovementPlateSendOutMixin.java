package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.blocks.MovementPlateBlock;
import com.pixelmonmod.pixelmon.entities.pokeballs.OccupiedPokeBallEntity;
import net.minecraft.block.BlockState;import net.minecraft.world.World;import net.minecraft.util.math.BlockPos;import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Send-out animation balls are not passengers on a movement plate. */
@Mixin(value=MovementPlateBlock.class,remap=false)
public abstract class MovementPlateSendOutMixin {
 @Inject(method="func_196262_a",at=@At("HEAD"),cancellable=true)
 private void backport$sendOut(BlockState state,World world,BlockPos pos,Entity entity,CallbackInfo ci){if(entity instanceof OccupiedPokeBallEntity)ci.cancel();}
}
