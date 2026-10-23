package local.pixelmon.backport;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender;
/** Only DLC species added by this backport; the signature Pikachu stays male. */
public final class AddedLegendGender {
    private AddedLegendGender(){}
    public static boolean genderless(int dex){return dex>=1014&&dex<=1017 || dex>=1020&&dex<=1025;}
    public static Gender gender(Pokemon p,Gender requested){
        if(p.getSpecies()==null)return requested;
        int dex=p.getSpecies().getDex();
        return dex==CapSpecies.DEX?Gender.MALE:genderless(dex)?Gender.NONE:requested;
    }
    public static void normalize(JsonObject species){
        if(!genderless(species.get("dex").getAsInt()))return;
        for(JsonElement e:species.getAsJsonArray("forms")){
            JsonObject form=e.getAsJsonObject();form.addProperty("malePercentage",-1);
            JsonArray genders=new JsonArray();genders.add("NONE");form.add("possibleGenders",genders);
        }
    }
    public static void loaded(Pokemon p){if(p.getSpecies()!=null&&genderless(p.getSpecies().getDex()))p.setGender(Gender.NONE);}
}
