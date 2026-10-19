package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.events.CaptureEvent;
import com.pixelmonmod.pixelmon.api.events.spawning.SpawnEvent;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnActionPokemon;
import net.minecraftforge.eventbus.api.*;

/** Keep the internal zero-dex sentinel registered, but exclude it from gameplay acquisition. */
public final class InvalidSpeciesSeal {
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void spawn(SpawnEvent event){
        if(event.action instanceof SpawnActionPokemon){var pokemon=((SpawnActionPokemon)event.action).pokemon;
            if(pokemon==null || pokemon.getSpecies().getDex()<=0)event.setCanceled(true);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void capture(CaptureEvent.StartCapture event){
        if(event.getPokemon()!=null && event.getPokemon().getPokemon().getSpecies().getDex()<=0)event.setCanceled(true);
    }
}
