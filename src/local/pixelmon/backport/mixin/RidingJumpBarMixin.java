package local.pixelmon.backport.mixin;
import local.pixelmon.backport.RidingMotion;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=ClientPlayerEntity.class,remap=false)
public abstract class RidingJumpBarMixin {
    @Inject(method="func_110317_t",at=@At("HEAD"),cancellable=true)
    private void backport$noHorseCharge(CallbackInfoReturnable<Boolean> ci){if(RidingMotion.active(((ClientPlayerEntity)(Object)this).func_184187_bx()))ci.setReturnValue(false);}
}
