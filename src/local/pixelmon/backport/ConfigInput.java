package local.pixelmon.backport;
import com.google.gson.*;

/** Convert friendly controls to the existing validated wire values. */
public final class ConfigInput {
    private ConfigInput(){}
    public static String display(JsonElement v){
        if(v.isJsonArray())return "列表（"+v.getAsJsonArray().size()+" 项）";
        return v.isJsonPrimitive()?ConfigLabels.value(v.getAsString()):"";
    }
    public static String edit(JsonElement v){
        if(!v.isJsonArray())return v.isJsonPrimitive()?v.getAsString():"";
        StringBuilder b=new StringBuilder();for(JsonElement item:v.getAsJsonArray()){
            if(b.length()>0)b.append("；");b.append(item.getAsString());
        }return b.toString();
    }
    public static JsonElement parse(JsonObject row,String text){
        JsonElement original=row.get("value");
        // Never reinterpret a list merely by selecting and saving it.
        if(text.equals(edit(original)))return original;
        String type=row.get("type").getAsString();
        if(type.equals("String"))return new JsonPrimitive(text);
        if(original.isJsonArray()){
            JsonArray result=new JsonArray();String item=row.get("itemType").getAsString();
            if(!text.trim().isEmpty())for(String token:text.split("[;；]",-1)){
                if(item.equals("String"))result.add(token.trim());
                else if(item.equals("Boolean")){
                    if(!token.trim().equals("true")&&!token.trim().equals("false"))throw new IllegalArgumentException("开关列表只能填写 true 或 false");
                    result.add(Boolean.parseBoolean(token.trim()));
                }else result.add(number(token.trim()));
            }return result;
        }
        if(original.isJsonPrimitive()&&original.getAsJsonPrimitive().isNumber())return number(text.trim());
        return new JsonPrimitive(text);
    }
    private static JsonPrimitive number(String text){
        try{return new JsonPrimitive(new java.math.BigDecimal(text));}
        catch(NumberFormatException e){throw new IllegalArgumentException("请输入数字");}
    }
}
