package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.mixin.BattleStatsOwnerAccess;
/** The native BattleStats copy constructor copies values, but leaves its owner null. */
public final class NpcSimulationCopy {
    private NpcSimulationCopy(){}
    public static PixelmonWrapper of(PixelmonWrapper source){
        PixelmonWrapper copy=new PixelmonWrapper(source);copy.pokemon=PokemonFactory.copy(source.pokemon);copy.entity=null;copy.bc=source.bc;
        ((BattleStatsOwnerAccess)copy.getBattleStats()).backport$owner(copy);
        // The native copy retains a temporary level's DelegateLink to the live
        // Pokemon. Rebind it to our detached Pokemon and preserve current HP.
        copy.setTempLevel(source.getPokemonLevelNum());copy.setHealth(source.getHealth());
        return copy;
    }
}
