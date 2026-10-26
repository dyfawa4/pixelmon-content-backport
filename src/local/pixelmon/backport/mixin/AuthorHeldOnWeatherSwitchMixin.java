package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.items.HeldItem;
import local.pixelmon.backport.AuthorHeldItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Native effect implementations, including direct getHeldItem call sites. */
@Mixin(targets={"com.pixelmonmod.pixelmon.items.heldItems.BoosterEnergyItem"},remap=false)
public abstract class AuthorHeldOnWeatherSwitchMixin {
    @Inject(method="onWeatherSwitch",at=@At("HEAD"),cancellable=true,require=1)
    private void backport$gate(com.pixelmonmod.pixelmon.battles.controller.BattleController a0, PixelmonWrapper a1, com.pixelmonmod.pixelmon.battles.status.Weather a2, CallbackInfo ci){if(AuthorHeldItems.blocked(a1,(HeldItem)(Object)this,"onWeatherSwitch"))ci.cancel();}
}
