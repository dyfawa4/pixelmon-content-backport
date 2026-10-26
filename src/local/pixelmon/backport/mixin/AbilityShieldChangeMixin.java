package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.ability.Ability;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.AbilityShield;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=PixelmonWrapper.class,remap=false)
public abstract class AbilityShieldChangeMixin {
    @Inject(method="setTempAbility(Lcom/pixelmonmod/pixelmon/api/pokemon/ability/Ability;Z)V",at=@At("HEAD"),cancellable=true)
    private void backport$protect(Ability next,boolean formChange,CallbackInfo ci){var mon=(PixelmonWrapper)(Object)this;if(!formChange&&AbilityShield.active(mon)){AbilityShield.blocked(mon);ci.cancel();}}
}
