package local.pixelmon.backport;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
/** Known pack names bypass conflicting upstream language-pack aliases, in Chinese only. */
public final class CompassNames {
    public static final JsonObject DATA=Content.read("backport/compass_names.json");
    private CompassNames(){}
    public static String name(String group,ResourceLocation id){
        if(id==null || !"zh_cn".equalsIgnoreCase(Minecraft.func_71410_x().field_71474_y.field_74363_ab))return null;
        return lookup(group,group.equals("source")?id.func_110624_b():id.toString());
    }
    public static String lookup(String group,String id){
        JsonObject rows=DATA.getAsJsonObject(group);return rows!=null && rows.has(id)?rows.get(id).getAsString():null;
    }
}
