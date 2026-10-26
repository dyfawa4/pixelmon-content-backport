package local.pixelmon.backport;
import java.util.*;
import com.google.gson.*;
import net.minecraft.util.ResourceLocation;
public final class UtilityItemLoot {
    public static final List<String> ITEMS=List.of("pixelmon_backport:moving_agreement","pixelmon_backport:gender_potion");
    public static void apply(String type,Map<ResourceLocation,JsonElement> data){for(var entry:data.entrySet())if(entry.getValue().isJsonObject()){
        var root=entry.getValue().getAsJsonObject();
        if(type.equals("drops")&&entry.getKey().func_110623_a().equals("pokechestdrops")){
            GrowthLampLoot.chests(root,List.of("tier1","tier2"),ITEMS);
            GrowthLampLoot.chests(root,List.of("tier3","ultraSpace"),List.of("pixelmon:ultra_space_teleporter"));
        }
        if(type.equals("bosstiers")&&root.has("id")&&Set.of("common","uncommon","rare","epic").contains(root.get("id").getAsString()))GrowthLampLoot.boss(root,ITEMS);
    }}
}
