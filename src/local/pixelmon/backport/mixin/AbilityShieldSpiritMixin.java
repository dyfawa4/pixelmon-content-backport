package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.WanderingSpirit;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.AbilityShield;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=WanderingSpirit.class,remap=false)
public abstract class AbilityShieldSpiritMixin {
    @Inject(method="applyEffectOnContactTarget",at=@At("HEAD"),cancellable=true)
    private void backport$atomic(PixelmonWrapper user,PixelmonWrapper target,CallbackInfo ci){if(AbilityShield.active(user)||AbilityShield.active(target)){AbilityShield.blocked(AbilityShield.active(user)?user:target);ci.cancel();}}
}
