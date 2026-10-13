package local.pixelmon.backport.mixin;
import local.pixelmon.backport.BattleMotion;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=LivingEntity.class,remap=false)
public abstract class BattleMotionLivingMixin {
    @Inject(method="func_184616_r",at=@At("HEAD"),cancellable=true)
    private void backport$pauseElytra(CallbackInfo ci){if(BattleMotion.frozen((LivingEntity)(Object)this))ci.cancel();}
    @Inject(method="func_70682_h",at=@At("RETURN"),cancellable=true)
    private void backport$pauseBreath(int air,CallbackInfoReturnable<Integer> cir){if(BattleMotion.frozen((LivingEntity)(Object)this))cir.setReturnValue(air);}
}
