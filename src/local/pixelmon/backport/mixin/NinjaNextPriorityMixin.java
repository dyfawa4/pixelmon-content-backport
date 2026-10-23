package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=ImmutableAttack.class,remap=false)
public abstract class NinjaNextPriorityMixin {
    @Inject(method="getPriority",at=@At("RETURN"),cancellable=true)
    private void backport$nextTurn(PixelmonWrapper user,CallbackInfoReturnable<Integer> cir){cir.setReturnValue(cir.getReturnValueI()+local.pixelmon.backport.NinjaNextPriority.bonus(user));}
}
