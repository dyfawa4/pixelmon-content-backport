package local.pixelmon.backport.mixin;
import local.pixelmon.backport.Erosion;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=PixelmonWrapper.class,remap=false)
public abstract class ErosionHealthMixin {
    @Redirect(method={"getMaxHealth(Z)I","recalculateMaxHP"},at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/Pokemon;getStat(Lcom/pixelmonmod/pixelmon/api/pokemon/stats/BattleStatsType;)I"))
    private int health(Pokemon pokemon,BattleStatsType stat){return Erosion.stat((PixelmonWrapper)(Object)this,stat,pokemon.getStat(stat));}
}
