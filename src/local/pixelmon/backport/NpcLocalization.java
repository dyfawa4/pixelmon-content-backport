package local.pixelmon.backport;
import com.google.gson.*;
import java.util.*;
import net.minecraft.util.ResourceLocation;

/** Exact stock text mapping; keeps custom text, dialogue indices and services intact. */
public final class NpcLocalization {
    public static final JsonObject DATA=Content.read("backport/npc_localization.json");
    private NpcLocalization(){}
    public static String translate(String text,String locale){
        if(text==null || !"zh_cn".equalsIgnoreCase(locale))return text;
        for(String category:new String[]{"names","texts"}){var map=DATA.getAsJsonObject(category);if(map.has(text))return map.get(text).getAsString();}
        String[] lines=text.split("\n",-1);if(lines.length==1)return text;
        for(int i=0;i<lines.length;i++)lines[i]=translate(lines[i],locale);return String.join("\n",lines);
    }
    private static JsonElement mapped(JsonElement value){
        if(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString())return new JsonPrimitive(translate(value.getAsString(),"zh_cn"));
        if(value.isJsonArray()){JsonArray a=new JsonArray();for(JsonElement v:value.getAsJsonArray())a.add(mapped(v));return a;}
        if(value.isJsonObject()){JsonObject o=new JsonObject();for(var e:value.getAsJsonObject().entrySet())o.add(e.getKey(),mapped(e.getValue()));return o;}
        return value;
    }
    public static void merge(Map<ResourceLocation,JsonElement> resources){
        for(var entry:DATA.getAsJsonObject("definitions").entrySet()){
            ResourceLocation key=new ResourceLocation("pixelmon",entry.getKey());JsonElement original=resources.get(key);
            if(original==null)resources.put(key,new JsonParser().parse(entry.getValue().toString()));
            else{
                JsonObject copy=new JsonParser().parse(original.toString()).getAsJsonObject();
                // Only display strings. Preserve existing custom Chinese and all
                // skin, economy, profession, service, name/chat index metadata.
                JsonObject stock=DATA.getAsJsonObject("stock").getAsJsonObject(entry.getKey());
                for(String field:new String[]{"names","chat"})if(copy.has(field)){
                    if(stock!=null && stock.has(field) && stock.get(field).equals(copy.get(field)))copy.add(field,new JsonParser().parse(entry.getValue().getAsJsonObject().get(field).toString()));
                    else copy.add(field,mapped(copy.get(field)));
                }
                resources.put(key,copy);
            }
        }
    }
}
