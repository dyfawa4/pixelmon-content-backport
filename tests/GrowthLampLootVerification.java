import java.util.*;
import java.util.zip.ZipFile;
import java.nio.charset.StandardCharsets;
import com.google.gson.*;
import local.pixelmon.backport.GrowthLampLoot;
/** Small definition check; no client or world startup. */
public final class GrowthLampLootVerification {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static JsonObject read(ZipFile zip,String name)throws Exception{return new JsonParser().parse(new String(zip.getInputStream(zip.getEntry(name)).readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();}
    public static void main(String[] args)throws Exception{
        try(var zip=new ZipFile(args[0])){
            var chest=read(zip,"data/pixelmon/drops/pokechestdrops.json");String tier1=chest.get("tier1").toString();GrowthLampLoot.chests(chest);String once=chest.toString();GrowthLampLoot.chests(chest);check(once.equals(chest.toString())&&tier1.equals(chest.get("tier1").toString()),"Chest reload idempotent; tier1 untouched");
            for(String tier:List.of("tier2","tier3"))for(String item:GrowthLampLoot.ITEMS){int count=0;for(var e:chest.getAsJsonArray(tier))if((e.isJsonPrimitive()?e.getAsString():e.getAsJsonObject().get("type").getAsString()).equals(item))count++;check(count==1,"One equal-weight entry "+tier+" "+item);}
            for(String tier:List.of("epic","legendary","ultimate")){
                var root=read(zip,"data/pixelmon/bosstiers/"+tier+".json");GrowthLampLoot.boss(root);String first=root.toString();GrowthLampLoot.boss(root);check(first.equals(root.toString()),"Boss reload idempotent "+tier);
                for(String item:GrowthLampLoot.ITEMS){int count=0;for(var set:root.getAsJsonArray("dropSets"))for(var value:set.getAsJsonObject().getAsJsonArray("drops")){var row=value.getAsJsonObject();if(row.get("item").getAsString().equals(item)){count++;check(row.get("weight").getAsInt()==1,"Common native item weight "+tier);}}check(count==1,"One drop "+tier+" "+item);}
            }
        }
        System.out.println("GROWTH_LAMP_SIMPLE_PASS both tier2/tier3 chests and all3 boss tiers, ordinary weight1, no duplicates after reload; no client startup");
    }
}
