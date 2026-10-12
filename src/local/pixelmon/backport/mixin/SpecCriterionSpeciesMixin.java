package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.advancements.triggers.SpecTrigger;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import local.pixelmon.backport.SpeciesCriterionGuard;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=SpecTrigger.Instance.class,remap=false)
public abstract class SpecCriterionSpeciesMixin implements SpeciesCriterionGuard {
    @Unique private int backport$expectedDex;
    public void backport$expectedDex(int dex){backport$expectedDex=dex;}
    @Inject(method="test",at=@At("HEAD"),cancellable=true)
    private void backport$exactSpecies(Pokemon pokemon,CallbackInfoReturnable<Boolean> cir){
        if(backport$expectedDex>0 && (pokemon==null || !local.pixelmon.backport.SpeciesCriterionPolicy.accepts(backport$expectedDex,pokemon.getSpecies().getDex(),pokemon.isEgg())))cir.setReturnValue(false);
    }
}
