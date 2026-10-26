package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.ability.AbilityRegistry;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.AbilityShield;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=AbilityRegistry.class,remap=false)
public abstract class AbilityShieldIgnoreMixin {
    @Inject(method="ignoreAbility(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/BattleController;)Z",at=@At("HEAD"),cancellable=true)
    private static void backport$suppression(PixelmonWrapper mon,BattleController battle,CallbackInfoReturnable<Boolean> cir){if(AbilityShield.active(mon))cir.setReturnValue(false);}
    @Inject(method="ignoreAbility(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;)Z",at=@At("HEAD"),cancellable=true)
    private static void backport$bypass(PixelmonWrapper user,PixelmonWrapper target,CallbackInfoReturnable<Boolean> cir){if(AbilityShield.active(target))cir.setReturnValue(false);}
}
