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
        String pirate="This be the day you shall always remember as the day you almost caught Captain Ruford Tauros.";
        check(NpcLocalization.translate("Pirate Captain","zh_cn").equals("海盗船长"),"Embedded pirate title missing");
        check(NpcLocalization.translate(pirate,"zh_cn").contains("鲁福德·肯泰罗船长"),"Screenshot pirate page untranslated");
        check(NpcLocalization.translate(pirate,"en_us").equals(pirate),"Pirate English locale changed");
        String archie="As the head of Team Aqua, I have dedicated myself to thwarting Magma's plans.";
        check(NpcLocalization.translate("Archie","zh_cn").equals("水梧桐"),"Archie title missing");
        check(NpcLocalization.translate(archie,"zh_cn").contains("水舰队首领"),"Archie dialogue missing");
        check(NpcLocalization.translate(archie,"en_us").equals(archie),"Archie English changed");
        check(NpcLocalization.translate("You have drowned underneath my power!","zh_cn").contains("淹没"),"Embedded trainer victory missing");
        check(local.pixelmon.backport.client.NpcDialogueDisplay.applies("com.pixelmonmod.pixelmon.client.gui.custom.dialogue.DialogueScreen"),"Screenshot uses generic dialogue window, not ChattingNPCScreen");
        check(local.pixelmon.backport.client.NpcDialogueDisplay.applies("com.pixelmonmod.pixelmon.client.gui.npc.ShopkeeperScreen"),"Shop screen excluded");
        check(!local.pixelmon.backport.client.NpcDialogueDisplay.applies("com.pixelmonmod.pixelmon.client.gui.custom.selection.SelectionScreen"),"Unrelated selection UI translated");
        check(NpcLocalization.translate("Aaaarrrrgggghhhh!\n"+pirate,"zh_cn").contains("记住今天"),"Serialized multi-line pirate chat untranslated");
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
        System.out.println("PASS 178 NPC definitions, Fred and serialized pirate name/dialogue, locale boundaries, old-save display copy and preserved service metadata; no game initialized.");
    }
}
