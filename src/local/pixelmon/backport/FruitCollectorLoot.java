package local.pixelmon.backport;
import java.util.*;
import com.google.gson.*;
import net.minecraft.util.ResourceLocation;
public final class FruitCollectorLoot {
 public static void apply(String type,Map<ResourceLocation,JsonElement> data){
  if(!"bosstiers".equals(type))return;
  for(var value:data.values())if(value.isJsonObject()){var root=value.getAsJsonObject();if(root.has("id")&&Set.of("epic","legendary","ultimate","drowned").contains(root.get("id").getAsString()))GrowthLampLoot.boss(root,List.of("pixelmon_backport:fruit_collector"));}
 }
}
