package local.pixelmon.backport;
import java.util.*;
import com.google.gson.*;
import net.minecraft.util.ResourceLocation;
/** Ordinary pool entries; independent Master Ball gates remain outside these pools. */
public final class GrowthLampLoot {
    private GrowthLampLoot(){}
    public static final List<String> ITEMS=List.of("pixelmon_backport:shrinking_lamp","pixelmon_backport:enlarging_lamp");
    public static void apply(String type,Map<ResourceLocation,JsonElement> data){
        for(var row:data.entrySet())if(row.getValue().isJsonObject()){
            var root=row.getValue().getAsJsonObject();
            if("drops".equals(type)&&row.getKey().func_110623_a().equals("pokechestdrops"))chests(root);
            if("bosstiers".equals(type)&&root.has("id")&&Set.of("epic","legendary","ultimate").contains(root.get("id").getAsString()))boss(root);
        }
    }
    public static void chests(JsonObject root){
        chests(root,List.of("tier2","tier3"),ITEMS);
    }
    public static void chests(JsonObject root,List<String> tiers,List<String> items){
        for(String tier:tiers){
            if(!root.has(tier))root.add(tier,new JsonArray());var pool=root.getAsJsonArray(tier);
            for(String item:items){boolean found=false;for(var value:pool)if(value.isJsonPrimitive()?item.equals(value.getAsString()):value.isJsonObject()&&value.getAsJsonObject().has("type")&&item.equals(value.getAsJsonObject().get("type").getAsString()))found=true;
                if(!found){var row=new JsonObject();row.addProperty("type",item);pool.add(row);}
            }
        }
    }
    public static void boss(JsonObject root){
        boss(root,ITEMS);
    }
    public static void boss(JsonObject root,List<String> items){
        if(!root.has("dropSets"))root.add("dropSets",new JsonArray());var sets=root.getAsJsonArray("dropSets");JsonArray target=null;int largest=-1;var existing=new HashSet<String>();
        for(var value:sets){var set=value.getAsJsonObject();if(!set.has("drops"))continue;var pool=set.getAsJsonArray("drops");
            for(var drop:pool)if(drop.getAsJsonObject().has("item"))existing.add(drop.getAsJsonObject().get("item").getAsString());
            if((!set.has("chance")||set.get("chance").getAsDouble()>0)&&pool.size()>largest){target=pool;largest=pool.size();}
        }
        if(target==null){var set=new JsonObject();set.addProperty("chance",1);set.addProperty("quantity",1);target=new JsonArray();set.add("drops",target);sets.add(set);}
        var weights=new TreeMap<Integer,Integer>();for(var value:target){var row=value.getAsJsonObject();int w=row.has("weight")?row.get("weight").getAsInt():1;if(w>0)weights.merge(w,1,Integer::sum);}
        int weight=1,frequency=0;for(var entry:weights.entrySet())if(entry.getValue()>frequency){weight=entry.getKey();frequency=entry.getValue();}
        for(String item:items)if(!existing.contains(item)){var row=new JsonObject();row.addProperty("weight",weight);row.addProperty("name","");row.addProperty("item",item);row.add("commands",new JsonArray());row.addProperty("ephemeral",false);target.add(row);}
    }
}
