package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Ignore only map rain at the native world-to-battle import, including mid-battle updates. */
@Mixin(value=BattleController.class,remap=false)
public abstract class NaturalRainBattleMixin {
 @Inject(method="calculateEntitiesAffectedByPrecipitation",at=@At("RETURN"),cancellable=true)
 private void backport$noNaturalRain(CallbackInfoReturnable<int[]> cir){int[] counts=cir.getReturnValue();cir.setReturnValue(new int[]{counts[0],0});}
}
