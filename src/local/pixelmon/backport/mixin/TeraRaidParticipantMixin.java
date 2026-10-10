package local.pixelmon.backport.mixin;
import local.pixelmon.backport.TeraRaidParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=RaidPixelmonParticipant.class,remap=false)
public abstract class TeraRaidParticipantMixin {
    @Inject(method="setDynamax",at=@At("HEAD"),cancellable=true)
    private void backport$noMax(PixelmonWrapper p,CallbackInfo ci){if((Object)this instanceof TeraRaidParticipant)ci.cancel();}
}
