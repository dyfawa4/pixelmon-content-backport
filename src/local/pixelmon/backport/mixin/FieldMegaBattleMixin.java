package local.pixelmon.backport.mixin;
import local.pixelmon.backport.FieldMega;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=PixelmonWrapper.class,remap=false)
public abstract class FieldMegaBattleMixin {
    // Before native wrapper caches stats, ability, types and its original form.
    @Inject(method="loadFromPokemon",at=@At("HEAD"))
    private void backport$beforeBattle(Pokemon p,CallbackInfo ci){FieldMega.restore(p);}
}
