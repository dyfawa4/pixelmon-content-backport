package local.pixelmon.backport;
import java.util.Map;
import com.google.gson.*;
import net.minecraft.util.ResourceLocation;
/** Extend selected native star pools before deserialization, leaving other stars intact. */
public final class MaxRaidRewards {
 public static final JsonObject ADDITIONS=Content.read("backport/max_raid_reward_additions.json");
 private MaxRaidRewards(){}
 public static void apply(Map<ResourceLocation,JsonElement> data){
  for(var row:data.entrySet())if(row.getKey().func_110623_a().equals("raiddrops")&&row.getValue().isJsonObject()){
   var root=row.getValue().getAsJsonObject();
   if(root.has("1"))for(var typed:root.getAsJsonObject("1").entrySet()){
    JsonArray pool=typed.getValue().getAsJsonArray();double berries=0;
    for(var value:pool){var entry=value.getAsJsonObject();if(entry.has("item")&&entry.get("item").getAsString().endsWith("_berry"))berries+=entry.get("weight").getAsDouble();}
    if(berries>0)for(String ore:new String[]{"pixelmon:dynite_ore","pixelmon:armorite_ore"}){JsonObject found=null;for(var value:pool)if(value.getAsJsonObject().has("item")&&ore.equals(value.getAsJsonObject().get("item").getAsString()))found=value.getAsJsonObject();if(found==null){found=new JsonObject();found.addProperty("item",ore);pool.add(found);}found.addProperty("weight",berries);}
   }
   if(!root.has("5"))root.add("5",new JsonObject());var five=root.getAsJsonObject("5");if(!five.has("Any"))five.add("Any",new JsonArray());var common=five.getAsJsonArray("Any");
   for(var extra:ADDITIONS.entrySet()){
    JsonObject existing=null;for(var value:common){var entry=value.getAsJsonObject();if(entry.has("item")&&entry.get("item").getAsString().equals(extra.getKey()))existing=entry;}
    if(existing==null){existing=new JsonObject();existing.addProperty("item",extra.getKey());common.add(existing);}existing.addProperty("weight",extra.getValue().getAsInt());
   }
  }
 }
}
