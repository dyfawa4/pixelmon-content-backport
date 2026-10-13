package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.advancements.PixelmonAdvancements;
import net.minecraft.entity.player.ServerPlayerEntity;

/** Check the native species criteria on actual acquisition; never forge catch statistics. */
public final class AcquisitionAdvancements {
    private AcquisitionAdvancements(){}
    public static boolean ownedStored(ServerPlayerEntity player,Pokemon pokemon){
        var server=net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();
        if(server==null || !server.func_213162_bc() || player==null || player.field_70170_p.field_72995_K || pokemon==null || pokemon.isEgg()
            || !player.func_110124_au().equals(pokemon.getOwnerPlayerUUID()))return false;
        var storage=pokemon.getStorage();
        if(storage instanceof com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage){
            if(storage!=com.pixelmonmod.pixelmon.api.storage.StorageProxy.getParty(player))return false;
        }else if(storage instanceof com.pixelmonmod.pixelmon.api.storage.PCStorage){
            if(storage!=com.pixelmonmod.pixelmon.api.storage.StorageProxy.getPCForPlayer(player))return false;
        }else return false;
        return pokemon.getPosition()!=null && storage.get(pokemon.getPosition())==pokemon;
    }
    public static void species(ServerPlayerEntity player,Pokemon pokemon){
        if(!ownedStored(player,pokemon))return;
        PixelmonAdvancements.LEGENDARY_CAPTURE_TRIGGER.trigger(player,pokemon);
        PixelmonAdvancements.SPEC_TRIGGER.trigger(player,pokemon);
        LegendAdvancements.capture(player,pokemon);
    }
    public static void evolved(ServerPlayerEntity player,Pokemon pokemon,int beforeDex){
        if(pokemon==null || !AcquisitionPolicy.evolved(beforeDex,pokemon.getSpecies().getDex(),pokemon.isEgg(),true,true,ownedStored(player,pokemon)))return;
        species(player,pokemon);
    }
    public static void raidStored(ServerPlayerEntity player,Pokemon pokemon){
        if(!ownedStored(player,pokemon))return;
        // Native raid success is posted BEFORE a cancellable storage operation.
        // This hook is called only after that operation reports a real party/PC insert.
        PixelmonAdvancements.throwCaptureTriggers(player,pokemon.getBall(),pokemon);
        PixelmonAdvancements.POKEDEX_TRIGGER.trigger(player);
        LegendAdvancements.capture(player,pokemon);
    }
}
