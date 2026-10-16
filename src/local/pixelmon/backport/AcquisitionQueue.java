package local.pixelmon.backport;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/** Defer pre-insert storage setters until the canonical server insert has finished. */
public final class AcquisitionQueue {
    private static final Queue<Pokemon> PENDING=new ConcurrentLinkedQueue<>();
    private static final Queue<UUID> LOGINS=new ConcurrentLinkedQueue<>();
    public static void stored(Pokemon pokemon){
        var server=ServerLifecycleHooks.getCurrentServer();if(server==null || !server.func_213162_bc() || pokemon==null)return;
        var storage=pokemon.getStorage();if(storage instanceof com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage || storage instanceof com.pixelmonmod.pixelmon.api.storage.PCStorage || storage instanceof com.pixelmonmod.pixelmon.api.storage.PCBox)PENDING.add(pokemon);
    }
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getPlayer() instanceof ServerPlayerEntity)LOGINS.add(event.getPlayer().func_110124_au());}
    @SubscribeEvent public void hatched(com.pixelmonmod.pixelmon.api.events.EggHatchEvent.Post event){stored(event.getPokemon());}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public void received(com.pixelmonmod.pixelmon.api.events.PokemonReceivedEvent event){if(!event.isCanceled())stored(event.getPokemon());}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
        var server=ServerLifecycleHooks.getCurrentServer();if(event.phase!=TickEvent.Phase.END || server==null || !server.func_213162_bc())return;
        for(UUID id;(id=LOGINS.poll())!=null;){var player=server.func_184103_al().func_177451_a(id);if(player==null)continue;
            for(Pokemon pokemon:StorageProxy.getParty(player).getAll())stored(pokemon);
            StorageProxy.getPCForPlayer(player).forEach((pokemon,position)->stored(pokemon));
        }
        Set<Pokemon> checked=Collections.newSetFromMap(new IdentityHashMap<>());
        for(int i=0;i<128;i++){Pokemon pokemon=PENDING.poll();if(pokemon==null)break;if(!checked.add(pokemon))continue;
            if(pokemon.isEgg())continue;ServerPlayerEntity player=pokemon.getOwnerPlayer();
            if(AcquisitionAdvancements.ownedStored(player,pokemon))AcquisitionAdvancements.species(player,pokemon);
        }
    }
    @SubscribeEvent public void stopped(net.minecraftforge.fml.event.server.FMLServerStoppedEvent event){PENDING.clear();LOGINS.clear();}
}
