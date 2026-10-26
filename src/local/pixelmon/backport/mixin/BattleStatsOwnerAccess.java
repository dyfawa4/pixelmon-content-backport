package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStats;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=BattleStats.class,remap=false)
public interface BattleStatsOwnerAccess {
    @Accessor("pixelmon") void backport$owner(PixelmonWrapper owner);
}
