package local.pixelmon.backport;
import java.util.*;
import com.google.gson.*;
import net.minecraft.util.ResourceLocation;
public final class MysteriousPotionLoot {
    public static final String ITEM=Content.NS+":mysterious_potion";
    public static void apply(String type,Map<ResourceLocation,JsonElement> data){for(var entry:data.entrySet())if(entry.getValue().isJsonObject()){
        var root=entry.getValue().getAsJsonObject();
        if(type.equals("drops")&&entry.getKey().func_110623_a().equals("pokechestdrops"))GrowthLampLoot.chests(root,List.of("tier1","tier2","tier3","ultraSpace"),List.of(ITEM));
        // The native notboss definition has no Boss identity and must not gain rewards.
        if(type.equals("bosstiers")&&root.has("id")&&!root.get("id").getAsString().equals("notboss"))GrowthLampLoot.boss(root,List.of(ITEM));
    }}
}
