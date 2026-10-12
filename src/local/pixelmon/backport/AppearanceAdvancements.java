package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.api.events.CaptureEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.advancements.PixelmonAdvancements;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Replay the native criterion for real captures and completed acquisition paths. */
public final class AppearanceAdvancements {
    private static boolean rainbow(Pokemon pokemon){return pokemon!=null && !pokemon.isEgg() && pokemon.isPalette("rainbow");}
    private static boolean zombie(Pokemon pokemon){return pokemon!=null && !pokemon.isEgg() && pokemon.isPalette("zombie");}
    private static boolean spiky(Pokemon pokemon){return pokemon!=null && !pokemon.isEgg() && pokemon.getSpecies().getDex()==172 && pokemon.getForm().isForm("spiky");}
    private static boolean server(ServerPlayerEntity player){
        var server=net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();
        return player!=null && !player.field_70170_p.field_72995_K && server!=null && server.func_213162_bc();
    }
    private static boolean stored(ServerPlayerEntity player,Pokemon pokemon){
        if(!server(player) || pokemon==null || pokemon.isEgg() || !player.func_110124_au().equals(pokemon.getOwnerPlayerUUID()))return false;
        var storage=pokemon.getStorage();
        return storage!=null && pokemon.getPosition()!=null && storage.get(pokemon.getPosition())==pokemon;
    }
    public static void rainbowConverted(ServerPlayerEntity player,Pokemon pokemon){
        if(rainbow(pokemon) && stored(player,pokemon))PixelmonAdvancements.SPEC_TRIGGER.trigger(player,pokemon);
    }
    public static void zombieConverted(ServerPlayerEntity player,Pokemon pokemon){
        if(zombie(pokemon) && stored(player,pokemon))PixelmonAdvancements.SPEC_TRIGGER.trigger(player,pokemon);
    }
    public static void spikyReceived(ServerPlayerEntity player,Pokemon pokemon){
        if(spiky(pokemon) && pokemon.getPersistentData().func_74767_n("Pichu_friend") && stored(player,pokemon))
            PixelmonAdvancements.SPEC_TRIGGER.trigger(player,pokemon);
    }
    private static void captured(ServerPlayerEntity player,Pokemon pokemon){
        if(server(player) && (rainbow(pokemon) || zombie(pokemon) || spiky(pokemon)))PixelmonAdvancements.SPEC_TRIGGER.trigger(player,pokemon);
    }
    @SubscribeEvent public void capture(CaptureEvent.SuccessfulCapture event){captured(event.getPlayer(),event.getPokemon().getPokemon());}
    @SubscribeEvent public void raid(CaptureEvent.SuccessfulRaidCapture event){captured(event.getPlayer(),event.getRaidPokemon());}
}
