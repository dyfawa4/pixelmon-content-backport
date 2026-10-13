package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="com.pixelmonmod.pixelmon.api.advancements.triggers.LegendaryCaptureTrigger$Instance",remap=false)
public abstract class LegendaryBirdCriterionMixin {
    @Shadow @Final private String name;
    @Inject(method="test",at=@At("HEAD"),cancellable=true)
    private void backport$ordinary(Pokemon pokemon,CallbackInfoReturnable<Boolean> ci){
        if(pokemon!=null && !local.pixelmon.backport.LegendaryCriterionPolicy.accepts(name,pokemon.getSpecies().getDex(),pokemon.getForm().getName(),pokemon.isEgg()))ci.setReturnValue(false);
    }
}
