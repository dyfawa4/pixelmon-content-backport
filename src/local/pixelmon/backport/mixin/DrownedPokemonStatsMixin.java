package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=Pokemon.class,remap=false)
public abstract class DrownedPokemonStatsMixin {
 @Inject(method="getStat",at=@At("RETURN"),cancellable=true,require=1)
 private void backport$persistentErosion(BattleStatsType stat,CallbackInfoReturnable<Integer> cir){cir.setReturnValue(local.pixelmon.backport.DrownedErosion.stat((Pokemon)(Object)this,stat,cir.getReturnValue()));}
}
