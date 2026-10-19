package local.pixelmon.backport;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import net.minecraft.entity.player.ServerPlayerEntity;

/** Quest-backed allowlist, separate from the generic all-species palette milestones. */
public final class SpecialPaletteAdvancements {
    public static final JsonArray DATA=Content.read("backport/special_palette_advancements.json").getAsJsonArray("appearances");
    private SpecialPaletteAdvancements(){}
    public static boolean matches(JsonObject row,int dex,String palette,String form,boolean egg,boolean shiny){
        return !egg && !shiny && row.get("dex").getAsInt()==dex && row.get("palette").getAsString().equals(palette) && row.get("form").getAsString().equals(form);
    }
    public static void acquired(ServerPlayerEntity owner,Pokemon pokemon){
        if(!AcquisitionAdvancements.ownedStored(owner,pokemon))return;
        for(JsonElement value:DATA){JsonObject row=value.getAsJsonObject();
            if(matches(row,pokemon.getSpecies().getDex(),pokemon.getPalette().getName(),pokemon.getForm().getName(),pokemon.isEgg(),pokemon.isShiny())){
                LegendAdvancements.award(owner,"root");LegendAdvancements.award(owner,"forms");LegendAdvancements.award(owner,row.get("id").getAsString());
            }
        }
    }
}
