package local.pixelmon.backport.mixin;
import local.pixelmon.backport.BurstCapture;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.catching.*;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import com.pixelmonmod.pixelmon.entities.pokeballs.*;
import com.pixelmonmod.pixelmon.battles.raids.RaidData;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Native ALWAYS_CAPTURE skips the ordinary checks entirely. */
@Mixin(targets="com.pixelmonmod.pixelmon.api.pokemon.catching.CaptureMethods$1",remap=false)
public abstract class CapGuaranteedCaptureMixin {
    @Inject(method="calculateCapture",at=@At("HEAD"),cancellable=true)
    private void backport$ban(CaptureHealth health,PokeBall ball,PokeBallEntity entity,RaidData raid,ServerPlayerEntity player,Pokemon pokemon,PokeBallMode mode,CallbackInfoReturnable<CaptureValues> cir){if(BurstCapture.forbidden(ball,pokemon))cir.setReturnValue(new CaptureValues(0,0));}
}
