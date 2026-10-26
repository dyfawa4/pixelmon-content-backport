package qa.mixin;
import qa.Update180R11Cases;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=net.minecraft.client.particle.ParticleManager.class,remap=false)
public abstract class R11ParticleProbe {
    @Inject(method="func_180533_a",at=@At("HEAD"))private void record(BlockPos pos,BlockState state,CallbackInfo ci){Update180R11Cases.particles(state);}
}
