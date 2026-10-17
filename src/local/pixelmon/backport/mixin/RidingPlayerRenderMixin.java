package local.pixelmon.backport.mixin;
import local.pixelmon.backport.client.RidingSeat;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=PlayerRenderer.class,remap=false)
public abstract class RidingPlayerRenderMixin {
    @Inject(method="func_225627_b_(Lnet/minecraft/client/entity/player/AbstractClientPlayerEntity;F)Lnet/minecraft/util/math/vector/Vector3d;",at=@At("RETURN"),cancellable=true)
    private void backport$visualSeat(AbstractClientPlayerEntity p,float partial,CallbackInfoReturnable<Vector3d> ci){ci.setReturnValue(ci.getReturnValue().func_178787_e(RidingSeat.offset(p,partial)));}
}
