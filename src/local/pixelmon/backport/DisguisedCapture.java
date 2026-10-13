package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
/** A transient per-ball success token, never an acquisition or saved Pokemon flag. */
public final class DisguisedCapture {
    private DisguisedCapture(){}
    public static boolean qualifies(boolean revealed,int dex,boolean stored){return revealed && dex==132 && stored;}
    public static void stored(ServerPlayerEntity player,Pokemon pokemon,boolean revealed){
        if(pokemon==null || !qualifies(revealed,pokemon.getSpecies().getDex(),AcquisitionAdvancements.ownedStored(player,pokemon)))return;
        var advancement=player.func_184102_h().func_191949_aK().func_192778_a(new ResourceLocation("pixelmon","specs/feebas_karp_trigger"));
        if(advancement!=null)player.func_192039_O().func_192750_a(advancement,"feebas_karp_spec_trigger");
    }
}
