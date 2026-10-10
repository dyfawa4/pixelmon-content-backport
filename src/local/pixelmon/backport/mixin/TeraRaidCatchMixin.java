package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.raids.RaidData;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=RaidData.RaidPlayer.class,remap=false)
public abstract class TeraRaidCatchMixin {
    @Shadow public Pokemon possibleCatch;
    @Shadow public boolean canCatch;
    @Inject(method="tryCatch",at=@At("HEAD"),cancellable=true)
    private void backport$guaranteed(RaidData raid,PokeBall ball,CallbackInfoReturnable<Integer> cir){if(canCatch && possibleCatch!=null && possibleCatch.getPersistentData().func_74767_n("BackportTeraRaidCatch"))cir.setReturnValue(4);}
}
