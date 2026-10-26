package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.TrainerParticipant;
import local.pixelmon.backport.NpcLead;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=TrainerParticipant.class,remap=false)
public abstract class NpcLeadMixin {
    @Inject(method="setPokemonInBattle",at=@At("HEAD")) private void backport$lead(CallbackInfo ci){NpcLead.choose((TrainerParticipant)(Object)this);}
}
