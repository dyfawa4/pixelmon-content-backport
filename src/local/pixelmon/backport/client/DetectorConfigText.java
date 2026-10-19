package local.pixelmon.backport.client;

import net.minecraft.client.Minecraft;

/** Use the compiled catalog even when the language loader omits a foreign namespace. */
public final class DetectorConfigText {
    private DetectorConfigText(){}
    public static String text(String key){
        if(key!=null && key.startsWith("farmersdelight.config."))return FarmersDelightConfigText.text(key);
        if(key==null || !key.startsWith("pixelmondetector.config."))return null;
        return DetectorConfigCatalog.get(key,Minecraft.func_71410_x().field_71474_y.field_74363_ab);
    }
    public static String label(Object holder){
        try{return text((String)Class.forName("com.mrcrayfish.configured.api.IConfigValue").getMethod("getTranslationKey").invoke(holder));}
        catch(ReflectiveOperationException failure){return null;}
    }
    public static String tooltipHeader(Object holder){
        String translated=label(holder);if(translated!=null)return translated;
        try{return (String)Class.forName("com.mrcrayfish.configured.api.IConfigValue").getMethod("getPath").invoke(holder);}
        catch(ReflectiveOperationException failure){return "";}
    }
    public static String configFileLabel(Object config){
        try{
            Class<?> api=Class.forName("com.mrcrayfish.configured.api.IModConfig");
            String id=(String)api.getMethod("getModId").invoke(config);
            if("farmersdelight".equals(id))return text("farmersdelight.config.file."+api.getMethod("getConfigType").invoke(config).toString().toLowerCase(java.util.Locale.ROOT));
            if(!"pixelmondetector".equals(id))return null;
            return text("pixelmondetector.config.file."+api.getMethod("getConfigType").invoke(config).toString().toLowerCase(java.util.Locale.ROOT));
        }catch(ReflectiveOperationException failure){return null;}
    }
}
