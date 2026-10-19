package local.pixelmon.backport;
import com.google.gson.*;
import java.util.*;

/** Normalize the fully merged data, retaining native stones and alternate origins. */
public final class ZaMegaData {
    private static final JsonObject ABILITIES=Content.read("backport/za-mega-abilities.json");
    private ZaMegaData(){}
    public static void normalize(JsonObject species){
        String dex=species.get("dex").getAsString();
        if(!ABILITIES.has(dex))return;
        JsonObject official=ABILITIES.getAsJsonObject(dex);
        Map<String,JsonObject> forms=new LinkedHashMap<>();
        for(JsonElement row:species.getAsJsonArray("forms")){JsonObject f=row.getAsJsonObject();forms.put(f.get("name").getAsString(),f);}
        Map<String,LinkedHashSet<String>> routes=new LinkedHashMap<>();
        for(JsonObject f:forms.values()){
            if(f.get("name").getAsString().startsWith("mega")||!f.has("megaItems")||!f.has("megas"))continue;
            JsonArray items=f.getAsJsonArray("megaItems"),destinations=f.getAsJsonArray("megas");
            if(items.size()!=destinations.size())throw new IllegalStateException("Unpaired Mega routes #"+dex);
            for(int i=0;i<items.size();i++)routes.computeIfAbsent(destinations.get(i).getAsString(),n->new LinkedHashSet<>()).add(items.get(i).getAsString());
        }
        for(Map.Entry<String,JsonObject> entry:forms.entrySet()){
            String name=entry.getKey();JsonObject form=entry.getValue();
            if(official.has(name)&&!official.get(name).isJsonNull()){
                JsonObject ability=new JsonObject();JsonArray values=new JsonArray();values.add(official.get(name));ability.add("abilities",values);ability.add("hiddenAbilities",new JsonArray());form.add("abilities",ability);
            }
            if(!name.startsWith("mega")||!routes.containsKey(name))continue;
            // Native Mega bosses choose both transformation and reward from these
            // paired lists. A transformed form must not inherit a sibling's stone.
            JsonArray items=new JsonArray(),destinations=new JsonArray();
            for(String item:routes.get(name)){items.add(item);destinations.add(name);}
            form.add("megaItems",items);form.add("megas",destinations);
        }
    }
}
