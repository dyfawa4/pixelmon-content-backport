package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Bulletproof;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=Bulletproof.class,remap=false)
public abstract class BulletproofMixin {
    @Inject(method="allowsIncomingAttack",at=@At("HEAD"),cancellable=true)
    private void backport$ball(PixelmonWrapper target,PixelmonWrapper user,Attack attack,CallbackInfoReturnable<Boolean> cir){
        if("Syrup Bomb".equals(attack.getMove().getAttackName()))cir.setReturnValue(false);
    }
}
