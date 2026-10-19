package local.pixelmon.backport.client;

import com.google.gson.JsonObject;
import local.pixelmon.backport.Content;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.ForgeConfigSpec;

/** Optional UI labels only; no dependency on Farmer's Delight classes. */
public final class FarmersDelightConfigText {
    private static final JsonObject ZH=Content.read("assets/farmersdelight/lang/zh_cn.json");
    private FarmersDelightConfigText(){}
    public static String text(String key){
        return "zh_cn".equals(Minecraft.func_71410_x().field_71474_y.field_74363_ab) && key!=null
            && key.startsWith("farmersdelight.config.") && ZH.has(key)?ZH.get(key).getAsString():null;
    }
    public static String folder(ForgeConfigSpec spec,boolean root,String name){
        Object value=spec.getRaw(java.util.Arrays.asList("settings","enableVanillaCropCrates"));
        if(!(value instanceof ForgeConfigSpec.ValueSpec))value=spec.getRaw(java.util.Arrays.asList("client","foodEffectTooltip"));
        if(!(value instanceof ForgeConfigSpec.ValueSpec)
            || ((ForgeConfigSpec.ValueSpec)value).getTranslationKey()==null
            || !((ForgeConfigSpec.ValueSpec)value).getTranslationKey().startsWith("farmersdelight.config."))return null;
        return text("farmersdelight.config.category."+(root?"root":name));
    }
}
