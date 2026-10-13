import local.pixelmon.backport.NpcLocalization;
import com.google.gson.*;
import net.minecraft.util.ResourceLocation;
import java.util.*;
public final class NpcLocalizationVerification {
    static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);}
    public static void main(String[] args){
        String text="Ah, Pokémon catching, are you? I prefer fish myself.";
        check(!NpcLocalization.translate(text,"zh_cn").equals(text),"Fred dialogue missing");
        check(!NpcLocalization.translate("Fred Fisher","zh_cn").equals("Fred Fisher"),"Fred name missing");
        check(NpcLocalization.translate(text,"en_us").equals(text),"English changed");
        check(NpcLocalization.translate("自定义中文","zh_cn").equals("自定义中文"),"Custom Chinese changed");
        check(NpcLocalization.translate("Custom unknown text","zh_cn").equals("Custom unknown text"),"Unknown custom text changed");
        check(NpcLocalization.DATA.getAsJsonObject("definitions").size()==178,"Missing registry definitions");
        Map<ResourceLocation,JsonElement> data=new HashMap<>();NpcLocalization.merge(data);check(data.size()==178,"Reload merge missing definitions");
        ResourceLocation key=new ResourceLocation("pixelmon","villagers/fisherman_zh_cn");JsonObject original=data.get(key).getAsJsonObject();original.addProperty("custom_service",42);
        JsonArray pages=new JsonArray();pages.add(text);original.add("chat",pages);NpcLocalization.merge(data);
        check(data.get(key).getAsJsonObject().get("custom_service").getAsInt()==42,"Service changed");
        check(original.getAsJsonArray("chat").get(0).getAsString().equals(text),"Stored pages mutated");
        check(!data.get(key).getAsJsonObject().getAsJsonArray("chat").get(0).getAsString().equals(text),"Old known page not translated");
        ResourceLocation bird=new ResourceLocation("pixelmon","trainers/ornithologist_zh_cn");
        data.put(bird,new JsonParser().parse(NpcLocalization.DATA.getAsJsonObject("stock").get("trainers/ornithologist_zh_cn").toString()));NpcLocalization.merge(data);
        check(data.get(bird).getAsJsonObject().getAsJsonArray("chat").size()==3,"Higher-priority native incomplete Chinese must gain all original dialogue groups");
        System.out.println("PASS 178 NPC definitions, Fred name/dialogue, locale boundaries, old-save display copy and preserved service metadata; no game initialized.");
    }
}
