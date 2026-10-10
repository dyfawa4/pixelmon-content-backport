package local.pixelmon.backport.mixin;
import local.pixelmon.backport.TeraRaidParticipant;
import com.pixelmonmod.pixelmon.battles.raids.RaidGovernor;
import com.pixelmonmod.pixelmon.battles.controller.participants.RaidPixelmonParticipant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=RaidGovernor.class,remap=false)
public abstract class TeraRaidGovernorMixin {
    @Inject(method="cycleDynamax",at=@At("HEAD"),cancellable=true)
    private void backport$noMaxTurn(RaidPixelmonParticipant participant,CallbackInfoReturnable<Integer> cir){if(participant instanceof TeraRaidParticipant)cir.setReturnValue(-1);}
}
