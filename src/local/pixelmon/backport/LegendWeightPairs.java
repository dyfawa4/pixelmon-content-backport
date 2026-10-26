package local.pixelmon.backport;
import java.util.*;
import com.google.gson.*;
import net.minecraft.util.ResourceLocation;
/** Base candidate weights only; no eligibility or global legendary attempt-rate changes. */
public final class LegendWeightPairs {
    private static String species(JsonObject row){return row.has("spec")?row.get("spec").getAsString().split(" ")[0].toLowerCase(Locale.ROOT):"";}
    public static void apply(Map<ResourceLocation,JsonElement> data){for(String[] pair:new String[][]{{"species:yveltal","species:ting-lu"},{"species:mew","species:fezandipiti"}}){Double weight=null;
        for(var value:data.values())if(value.isJsonObject()&&value.getAsJsonObject().has("spawnInfos"))for(var info:value.getAsJsonObject().getAsJsonArray("spawnInfos")){var row=info.getAsJsonObject();if(species(row).equals(pair[1])&&row.has("rarity")){double w=row.get("rarity").getAsDouble();if(w>0&&Double.isFinite(w))weight=weight==null?w:Math.max(weight,w);}}
        if(weight!=null)for(var value:data.values())if(value.isJsonObject()&&value.getAsJsonObject().has("spawnInfos"))for(var info:value.getAsJsonObject().getAsJsonArray("spawnInfos")){var row=info.getAsJsonObject();if(species(row).equals(pair[0])&&row.has("rarity")&&row.get("rarity").getAsDouble()>0)row.addProperty("rarity",weight);}
    }}
}
