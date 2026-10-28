package local.pixelmon.backport;
import com.google.gson.*;
import java.util.Map;
/** Localize only equal native requirements; a newer material/count cannot label an older recipe. */
public final class DayCareRequirementText {
    private DayCareRequirementText(){}
    private static final Map<String,String> NATIVE_NAMES=Map.of("minecraft:grass_block","草方块","minecraft:clay","黏土块");
    private static JsonObject mechanics(JsonObject row){
        JsonObject copy=new JsonParser().parse(row.toString()).getAsJsonObject();copy.remove("error_message");return copy;
    }
    public static void localize(JsonObject nativeRoot,JsonObject reference){
        if(!nativeRoot.has("requirements")||!reference.has("requirements"))return;
        for(JsonElement value:nativeRoot.getAsJsonArray("requirements")){
            JsonObject row=value.getAsJsonObject(),mechanics=mechanics(row);boolean matched=false;
            for(JsonElement source:reference.getAsJsonArray("requirements")){
                JsonObject candidate=source.getAsJsonObject();
                if(candidate.has("error_message")&&mechanics.equals(mechanics(candidate))){row.addProperty("error_message",candidate.get("error_message").getAsString());matched=true;break;}
            }
            if(matched||!row.has("type")||!row.get("type").getAsString().equals("itemstack")||!row.has("items"))continue;
            JsonArray items=row.getAsJsonArray("items");if(items.size()!=1)continue;
            JsonObject item=items.get(0).getAsJsonObject();
            if(item.has("item")&&item.has("amount")&&NATIVE_NAMES.containsKey(item.get("item").getAsString()))
                row.addProperty("error_message",item.get("amount").getAsString()+"x "+NATIVE_NAMES.get(item.get("item").getAsString()));
        }
    }
}
