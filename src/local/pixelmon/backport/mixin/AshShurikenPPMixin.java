package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.stats.Moveset;
import local.pixelmon.backport.AshShurikenPP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=Moveset.class,remap=false)
public abstract class AshShurikenPPMixin {
    @Inject(method={"healAllPP","toBytes","writeToNBT","tryNotifyPokemon"},at=@At("HEAD"))
    private void backport$applyPP(CallbackInfo ci){AshShurikenPP.apply((Moveset)(Object)this);}
    @Inject(method={"readFromNBT","fromBytes"},at=@At("RETURN"))
    private void backport$loadedPP(CallbackInfo ci){AshShurikenPP.apply((Moveset)(Object)this);}
    @Inject(method="withPokemon",at=@At("RETURN"))
    private void backport$ownerPP(CallbackInfoReturnable<Moveset> cir){AshShurikenPP.apply((Moveset)(Object)this);}
    @Inject(method="copy",at=@At("RETURN"))
    private void backport$copyPP(CallbackInfoReturnable<Moveset> cir){AshShurikenPP.apply(cir.getReturnValue(),((Moveset)(Object)this).pokemon);}
}
