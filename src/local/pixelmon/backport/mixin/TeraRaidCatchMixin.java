package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.raids.RaidData;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=RaidData.RaidPlayer.class,remap=false)
public abstract class TeraRaidCatchMixin {
    @Inject(method="winRaid",at=@At("RETURN"))
    private void backport$burstPreview(com.pixelmonmod.pixelmon.battles.controller.participants.RaidPixelmonParticipant opponent,RaidData raid,com.pixelmonmod.pixelmon.battles.raids.RaidGovernor governor,CallbackInfo ci){local.pixelmon.backport.BurstRaidCapture.preview(raid,(RaidData.RaidPlayer)(Object)this);}
    @Shadow public Pokemon possibleCatch;
    @Inject(method="tryCatch",at=@At("HEAD"),cancellable=true)
    private void backport$forbiddenBall(RaidData raid,PokeBall ball,CallbackInfoReturnable<Integer> cir){
        if(local.pixelmon.backport.BurstCapture.forbidden(ball,possibleCatch)){cir.setReturnValue(0);return;}
    }
}
