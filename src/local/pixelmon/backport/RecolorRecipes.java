package local.pixelmon.backport;
import java.util.*;
import com.google.gson.*;
import net.minecraft.util.ResourceLocation;

/** Remove all Pixelmon finished-item recolors, retaining recipes from raw materials. */
public final class RecolorRecipes {
 private static final String[] COLORS={"light_blue","light_gray","white","orange","magenta","yellow","lime","pink","gray","cyan","purple","blue","brown","green","red","black"};
 private RecolorRecipes(){}
 private static boolean dye(JsonObject value){String tag=value.has("tag")?value.get("tag").getAsString():"";String item=value.has("item")?value.get("item").getAsString():"";return tag.equals("forge:dyes")||tag.startsWith("forge:dyes/")||item.startsWith("minecraft:")&&item.endsWith("_dye");}
 private static String family(String item){if(!item.startsWith("pixelmon:"))return "";String path=item.substring(9);for(String color:COLORS)if(path.startsWith(color+"_"))return path.substring(color.length()+1);return path;}
 public static boolean blocked(JsonObject recipe){
  if(!recipe.has("type")||!recipe.has("result")||!recipe.get("result").isJsonObject())return false;
  String kind=recipe.get("type").getAsString();JsonArray ingredients=new JsonArray();
  if(kind.equals("minecraft:crafting_shapeless")&&recipe.has("ingredients"))ingredients=recipe.getAsJsonArray("ingredients");
  else if((kind.equals("minecraft:crafting_shaped")||kind.equals("pixelmon:crafting_shaped"))&&recipe.has("key")){for(var value:recipe.getAsJsonObject("key").entrySet())ingredients.add(value.getValue());}
  else return false;
  var result=recipe.getAsJsonObject("result");if(!result.has("item"))return false;String output=result.get("item").getAsString(),family=family(output);if(family.isEmpty())return false;
  boolean hasDye=false;for(var value:ingredients)if(value.isJsonObject()&&dye(value.getAsJsonObject()))hasDye=true;if(!hasDye)return false;
  for(var value:ingredients){if(!value.isJsonObject())continue;var finished=value.getAsJsonObject();
   if(finished.has("item")&&family.equals(family(finished.get("item").getAsString())))return true;
   if(finished.has("tag")&&finished.get("tag").getAsString().equals("pixelmon:"+family+"s"))return true;
  }return false;
 }
 public static void remove(Map<ResourceLocation,JsonElement> recipes){recipes.entrySet().removeIf(entry->entry.getValue().isJsonObject()&&blocked(entry.getValue().getAsJsonObject()));}
}
