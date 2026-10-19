package local.pixelmon.backport;

import com.google.gson.*;
import java.util.Map;
import net.minecraft.util.ResourceLocation;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;

/** An earned permanent form, using the legacy Ash model and ability unchanged. */
public final class PermanentAsh {
    public static final String FORM = "ash_permanent";
    public static final String MEGA = "mega_ash";
    private PermanentAsh() {}
    public static boolean isPermanent(PixelmonWrapper pokemon) {
        return pokemon.getSpecies().getDex()==658 && pokemon.getForm().isForm(FORM);
    }
    public static boolean isAsh(Stats stats) {return stats.isForm("ash",FORM,MEGA);}
    /** A distinct origin form shares the real Mega data, while preserving Ash eligibility. */
    public static void addMegaForm(JsonObject species){
        if(species.get("dex").getAsInt()!=658)return;
        JsonObject mega=null,base=null;
        for(JsonElement row:species.getAsJsonArray("forms")){
            JsonObject f=row.getAsJsonObject();String name=f.get("name").getAsString();
            if(name.equals("mega"))mega=f;if(name.isEmpty())base=f;
            if(name.equals(MEGA))return;
        }
        if(mega==null||base==null||!base.has("megaItems"))return;
        JsonObject ashMega=new JsonParser().parse(mega.toString()).getAsJsonObject();
        ashMega.addProperty("name",MEGA);ashMega.addProperty("defaultBaseForm",FORM);
        // The appearance, stats and changed ability are identical to ordinary Mega Greninja.
        JsonArray megaItems=new JsonArray(),megaTargets=new JsonArray();
        for(int i=0;i<base.getAsJsonArray("megas").size();i++)if(base.getAsJsonArray("megas").get(i).getAsString().equals("mega")){megaItems.add(base.getAsJsonArray("megaItems").get(i));megaTargets.add(MEGA);}
        ashMega.add("megaItems",megaItems);ashMega.add("megas",megaTargets);
        for(JsonElement row:species.getAsJsonArray("forms")){
            JsonObject f=row.getAsJsonObject();String name=f.get("name").getAsString();
            if(!name.equals("ash")&&!name.equals(FORM))continue;
            JsonArray items=new JsonArray(),destinations=new JsonArray();
            for(int i=0;i<base.getAsJsonArray("megas").size();i++)if(base.getAsJsonArray("megas").get(i).getAsString().equals("mega")){
                items.add(base.getAsJsonArray("megaItems").get(i));destinations.add(MEGA);
            }
            f.add("megaItems",items);f.add("megas",destinations);
        }
        species.getAsJsonArray("forms").add(ashMega);
    }
    public static void addForm(Map<ResourceLocation,JsonElement> data) {
        for(Map.Entry<ResourceLocation,JsonElement> entry:data.entrySet()) {
            JsonObject species=entry.getValue().getAsJsonObject();
            if(!species.has("dex") || species.get("dex").getAsInt()!=658)continue;
            JsonArray forms=species.getAsJsonArray("forms");JsonObject ash=null,existing=null;
            for(JsonElement form:forms) {
                String name=form.getAsJsonObject().get("name").getAsString();
                if(FORM.equals(name))existing=form.getAsJsonObject();
                if("ash".equals(name))ash=form.getAsJsonObject();
            }
            if(ash==null)throw new IllegalStateException("Legacy Ash Greninja form missing");
            JsonObject result=new JsonParser().parse(species.toString()).getAsJsonObject();
            JsonObject permanent=new JsonParser().parse(ash.toString()).getAsJsonObject();
            permanent.addProperty("name",FORM);
            permanent.addProperty("defaultBaseForm",FORM);
            JsonArray tags=new JsonArray();
            if(permanent.has("tags"))for(JsonElement tag:permanent.getAsJsonArray("tags"))if(!"temp".equals(tag.getAsString()))tags.add(tag);
            permanent.add("tags",tags);
            if(existing!=null){
                if(!permanent.equals(existing))throw new IllegalStateException("Reserved form collision: Greninja "+FORM);
                return;
            }
            result.getAsJsonArray("forms").add(permanent);
            entry.setValue(result);return;
        }
        throw new IllegalStateException("Legacy Greninja species missing");
    }
}
