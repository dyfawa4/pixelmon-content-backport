package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.status.GastroAcid;
import com.pixelmonmod.pixelmon.battles.controller.log.AttackResult;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.AbilityShield;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=GastroAcid.class,remap=false)
public abstract class AbilityShieldGastroMixin {
    @Inject(method="applyEffect",at=@At("HEAD"),cancellable=true)
    private void backport$protect(PixelmonWrapper user,PixelmonWrapper target,CallbackInfo ci){if(AbilityShield.active(target)){AbilityShield.blocked(target);if(user.attack!=null&&user.attack.moveResult!=null)user.attack.moveResult.result=AttackResult.failed;ci.cancel();}}
}
