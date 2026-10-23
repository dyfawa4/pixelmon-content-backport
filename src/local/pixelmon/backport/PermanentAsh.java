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
    public static boolean blockedOrdinaryStone(com.pixelmonmod.pixelmon.api.pokemon.Pokemon p){return p!=null&&p.getSpecies().getDex()==658&&p.getHeldItem().func_77973_b()==Backport.item("greninjite")&&(isAsh(p.getForm())||p.getForm().isForm("battle_bond")||p.getAbility() instanceof com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.BattleBond);}
    /** A distinct origin form shares the real Mega data, while preserving Ash eligibility. */
    public static void addMegaForm(JsonObject species){
        if(species.get("dex").getAsInt()!=658)return;
        JsonObject mega=null,base=null,existing=null;
        for(JsonElement row:species.getAsJsonArray("forms")){
            JsonObject f=row.getAsJsonObject();String name=f.get("name").getAsString();
            if(name.equals("mega"))mega=f;if(name.isEmpty())base=f;
            if(name.equals(MEGA))existing=f;
        }
        if(mega==null||base==null||!base.has("megaItems"))return;
        JsonArray baseItems=new JsonParser().parse(base.getAsJsonArray("megaItems").toString()).getAsJsonArray(),baseTargets=new JsonParser().parse(base.getAsJsonArray("megas").toString()).getAsJsonArray();
        JsonObject ashMega=existing==null?new JsonParser().parse(mega.toString()).getAsJsonObject():existing;
        ashMega.addProperty("name",MEGA);ashMega.addProperty("defaultBaseForm","");
        JsonObject abilities=new JsonObject();JsonArray drizzle=new JsonArray();drizzle.add("Drizzle");abilities.add("abilities",drizzle);abilities.add("hiddenAbilities",new JsonArray());ashMega.add("abilities",abilities);
        JsonArray megaItems=new JsonArray(),megaTargets=new JsonArray();
        megaItems.add(Content.NS+":ash_greninjite");megaTargets.add(MEGA);
        ashMega.add("megaItems",megaItems);ashMega.add("megas",megaTargets);
        for(JsonElement row:species.getAsJsonArray("forms")){
            JsonObject f=row.getAsJsonObject();String name=f.get("name").getAsString();
            if(!name.equals("ash")&&!name.equals(FORM)&&!name.isEmpty()&&!name.equals("base")&&!name.equals("battle_bond"))continue;
            JsonArray items=new JsonArray(),destinations=new JsonArray();
            if(!name.equals("ash")&&!name.equals(FORM)){
                JsonArray oldItems=f.has("megaItems")?f.getAsJsonArray("megaItems"):baseItems,oldTargets=f.has("megas")?f.getAsJsonArray("megas"):baseTargets;
                for(int i=0;i<oldTargets.size();i++)if(!oldTargets.get(i).getAsString().equals(MEGA)&&!(name.equals("battle_bond")&&oldItems.get(i).getAsString().equals(Content.NS+":greninjite"))){
                    items.add(oldItems.get(i));destinations.add(oldTargets.get(i));
                }
                items.add(Content.NS+":ash_greninjite");destinations.add(MEGA);
            }
            f.add("megaItems",items);f.add("megas",destinations);
        }
        if(existing==null)species.getAsJsonArray("forms").add(ashMega);
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
