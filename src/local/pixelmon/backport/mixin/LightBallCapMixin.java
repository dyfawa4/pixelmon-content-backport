package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.items.heldItems.StatEnhancingItems;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.CapSpecies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Extend only the native Light Ball species predicate; keep native multipliers and item suppression. */
@Mixin(value=StatEnhancingItems.class,remap=false)
public abstract class LightBallCapMixin {
    @Inject(method="canAffect",at=@At("HEAD"),cancellable=true)
    private void backport$cap(PixelmonWrapper pokemon,CallbackInfoReturnable<Boolean> cir){
        if((Object)this==PixelmonItems.light_ball&&pokemon.getSpecies().getDex()==CapSpecies.DEX)cir.setReturnValue(true);
    }
}
