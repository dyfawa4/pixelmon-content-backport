package local.pixelmon.backport;
import java.util.Map;
import com.google.gson.*;
import net.minecraft.util.ResourceLocation;
/** Normalize direct cobblestone crafting inputs; leave outputs, cooking and NBT inputs alone. */
public final class UltraMaterialRecipes {
 public static void apply(Map<ResourceLocation,JsonElement> recipes){for(var value:recipes.values())if(value.isJsonObject()){
  var recipe=value.getAsJsonObject();if(!recipe.has("type"))continue;String type=recipe.get("type").getAsString();
  if(type.equals("minecraft:crafting_shaped")&&recipe.has("key"))for(var entry:recipe.getAsJsonObject("key").entrySet())entry.setValue(ingredient(entry.getValue()));
  if(type.equals("minecraft:crafting_shapeless")&&recipe.has("ingredients")){var list=recipe.getAsJsonArray("ingredients");for(int i=0;i<list.size();i++)list.set(i,ingredient(list.get(i)));}
 }}
 private static JsonElement ingredient(JsonElement input){
  if(input.isJsonArray()){var list=input.getAsJsonArray();for(int i=0;i<list.size();i++)list.set(i,ingredient(list.get(i)));}
  if(input.isJsonObject()){var row=input.getAsJsonObject();if(row.size()==1&&row.has("item")&&row.get("item").getAsString().equals("minecraft:cobblestone")){var result=new JsonObject();result.addProperty("tag","forge:cobblestone");return result;}}
  return input;
 }
}
