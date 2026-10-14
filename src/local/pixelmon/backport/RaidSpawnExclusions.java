package local.pixelmon.backport;
import com.google.gson.*;
import java.util.*;
import com.pixelmonmod.pixelmon.init.registry.RaidRegistry;

/** A den-only exclusion list, independent from legendary village spawn rules. */
public final class RaidSpawnExclusions {
    private static final JsonArray EXCLUDED=Content.read("backport/raid_spawn_exclusions.json").getAsJsonArray("excludedSpecies");
    private RaidSpawnExclusions(){}
    public static boolean allowed(int dex){
        for(JsonElement row:EXCLUDED)if(row.getAsJsonObject().get("dex").getAsInt()==dex)return false;
        return true;
    }
    public static boolean allowedSpec(String spec){
        String token=spec.trim().split("\\s+",2)[0];
        for(JsonElement row:EXCLUDED){JsonObject species=row.getAsJsonObject();
            if(token.equalsIgnoreCase(species.get("name").getAsString())||token.equals(Integer.toString(species.get("dex").getAsInt())))return false;
        }
        return true;
    }
    public static Collection<RaidRegistry.Raid> candidates(Collection<RaidRegistry.Raid> source){
        if(source==null)return null;
        List<RaidRegistry.Raid> filtered=null;
        for(RaidRegistry.Raid row:source)if(row!=null&&!allowed(row.getSpecies())){filtered=new ArrayList<>();break;}
        if(filtered==null)return source;
        for(RaidRegistry.Raid row:source)if(row!=null&&allowed(row.getSpecies()))filtered.add(row);
        return filtered;
    }
}
