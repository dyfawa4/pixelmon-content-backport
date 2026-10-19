import com.google.gson.*;
import local.pixelmon.backport.ConfigEditor;
import local.pixelmon.backport.ConfigInput;
import local.pixelmon.backport.ConfigLabels;

/** User-facing values must round-trip without requiring JSON syntax. */
public final class ConfigInterfaceVerification {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static JsonObject row(String type,String json){JsonObject r=new JsonObject();r.addProperty("type",type);r.add("value",new JsonParser().parse(json));return r;}
    public static void main(String[] args){
        JsonObject number=row("int","5");
        check(ConfigInput.parse(number,"6").getAsInt()==6,"Plain numeric edit");
        try{ConfigInput.parse(number,"六");throw new AssertionError("Invalid number accepted");}catch(IllegalArgumentException expected){}
        JsonObject text=row("String","\"old\"");
        check(ConfigInput.parse(text,"中文文字\"不加引号").getAsString().equals("中文文字\"不加引号"),"Literal Chinese and quotes");
        JsonObject list=row("List","[\"minecraft:overworld\",\"保留；原字符串\"]");list.addProperty("itemType","String");
        check(ConfigInput.parse(list,ConfigInput.edit(list.get("value"))).equals(list.get("value")),"Unchanged list delimiters preserved");
        check(ConfigInput.parse(list,"minecraft:overworld；pixelmon:ultra_space").getAsJsonArray().size()==2,"Friendly list separator");
        check(ConfigInput.parse(list,"").getAsJsonArray().size()==0,"Empty list");
        check(ConfigInput.display(new JsonPrimitive(true)).equals("开启"),"Chinese toggle");
        check(ConfigLabels.text("ashWaterTurns").equals("水系强化持续回合"),"Backport Chinese label");
        JsonArray found=ConfigEditor.page("backport","种族值",0).getAsJsonArray("entries");
        check(found.size()==1&&found.get(0).getAsJsonObject().get("path").getAsString().equals("erosionReduction"),"Chinese search");
        check(found.get(0).getAsJsonObject().has("label"),"Labels included in server reply");
        check(ConfigEditor.page("backport","erosionTurns",0).getAsJsonArray("entries").size()==1,"English search retained");
        System.out.println("PASS Chinese configuration labels/search, friendly numbers/literal text/list round-trips, unchanged data and invalid input.");
    }
}
