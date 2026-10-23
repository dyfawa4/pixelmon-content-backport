package local.pixelmon.backport.mixin;

import local.pixelmon.backport.CapBallPreference;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.catching.*;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import com.pixelmonmod.pixelmon.entities.pokeballs.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** The shared native shake roll has its own guaranteed-ball shortcut. */
@Mixin(targets="com.pixelmonmod.pixelmon.api.pokemon.catching.CaptureMethods$6",remap=false)
public abstract class CapCaptureRollMixin {
 @Redirect(method="tryCapture",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/item/pokeball/PokeBall;isGuaranteedCatch()Z"))
 private boolean backport$capRoll(PokeBall ball,CaptureValues values,CaptureHealth health,PokeBall original,PokeBallEntity entity,ServerPlayerEntity player,Pokemon pokemon,PokeBallMode mode){return !CapBallPreference.cap(pokemon)&&ball.isGuaranteedCatch();}
}
