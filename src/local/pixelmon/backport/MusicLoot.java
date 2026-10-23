package local.pixelmon.backport;
import java.util.Map;
import com.google.gson.*;
import net.minecraft.util.ResourceLocation;
/** One equally weighted candidate in the existing disc pool, preserving its rolls and empty weight. */
public final class MusicLoot {
 public static final ResourceLocation TABLE=new ResourceLocation("pixelmon:chests/ultraspace/plant_monolith");
 public static final String DISC="pixelmon_backport:tera_raid_music_disc";
 public static final String WILD_TWO="pixelmon_backport:wild_music_disc_two";
 private MusicLoot(){}
 public static void apply(Map<ResourceLocation,JsonElement> tables){
  JsonElement raw=tables.get(TABLE);if(raw==null||!raw.isJsonObject())return;
  var table=raw.getAsJsonObject();if(!table.has("pools"))return;
  for(var value:table.getAsJsonArray("pools")){
   var pool=value.getAsJsonObject();if(!pool.has("entries"))continue;
   var entries=pool.getAsJsonArray("entries");JsonObject template=null;
   for(var e:entries){var entry=e.getAsJsonObject();String name=entry.has("name")?entry.get("name").getAsString():"";
    if(DISC.equals(name))return;
    if("minecraft:music_discs".equals(name)||name.startsWith("pixelmon:")&&name.endsWith("_music_disc"))template=entry;
   }
   if(template!=null){var entry=new JsonParser().parse(template.toString()).getAsJsonObject();entry.addProperty("type","minecraft:item");entry.addProperty("name",DISC);entry.remove("expand");entries.add(entry);return;}
  }
 }
}
