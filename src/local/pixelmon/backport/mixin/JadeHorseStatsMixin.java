package local.pixelmon.backport.mixin;
import local.pixelmon.backport.client.JadePixelmonCompat;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo @Mixin(targets="snownee.jade.addon.vanilla.HorseProvider",remap=false)
public abstract class JadeHorseStatsMixin {
    @Inject(method="appendBody",at=@At("HEAD"),cancellable=true)
    private void backport$skipPixelmonHorseStats(java.util.List<?> tooltip,@Coerce Object accessor,@Coerce Object config,CallbackInfo ci){
        if(JadePixelmonCompat.decorativeOrPokemon(accessor))ci.cancel();
    }
}
