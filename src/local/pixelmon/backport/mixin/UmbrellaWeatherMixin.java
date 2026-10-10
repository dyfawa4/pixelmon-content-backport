package local.pixelmon.backport.mixin;
import local.pixelmon.backport.UmbrellaHooks;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value={Sunny.class,Rainy.class},remap=false)
public abstract class UmbrellaWeatherMixin {
    @Inject(method="modifyPowerAndAccuracyTarget",at=@At("HEAD"),cancellable=true)
    private void backport$neutralWeather(int power,int accuracy,PixelmonWrapper user,PixelmonWrapper target,Attack attack,CallbackInfoReturnable<int[]> cir){if(UmbrellaHooks.holds(target))cir.setReturnValue(new int[]{power,accuracy});}
}
