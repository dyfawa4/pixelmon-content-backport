package local.pixelmon.backport.mixin;
import local.pixelmon.backport.BattleMotion;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=PlayerEntity.class,remap=false)
public abstract class BattleMotionPlayerMixin {
    @Inject(method="func_213352_e",at=@At("HEAD"),cancellable=true)
    private void backport$holdTravel(Vector3d input,CallbackInfo ci){PlayerEntity self=(PlayerEntity)(Object)this;if(BattleMotion.frozen(self)){BattleMotion.stop(self);ci.cancel();}}
}
