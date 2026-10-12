import java.util.*;
import com.google.gson.*;
import local.pixelmon.backport.SpeciesCriterionPolicy;
public final class EeveeFamilyVerification {
    static JsonObject data;
    static Set<String> captured=new HashSet<>();
    static void capture(int dex,boolean egg){
        for(var row:data.getAsJsonObject("criteria").entrySet()){
            int expected=row.getValue().getAsJsonObject().getAsJsonObject("conditions").get("backport_expected_dex").getAsInt();
            if(SpeciesCriterionPolicy.accepts(expected,dex,egg))captured.add(row.getKey());
        }
    }
    static boolean complete(){
        for(var requirement:data.getAsJsonArray("requirements")){
            boolean any=false;for(var key:requirement.getAsJsonArray())any|=captured.contains(key.getAsString());
            if(!any)return false;
        }return true;
    }
    public static void main(String[] args)throws Exception{
        try(var in=EeveeFamilyVerification.class.getClassLoader().getResourceAsStream("data/pixelmon/advancements/specs/eeveelution_capture_all.json")){
            data=new JsonParser().parse(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
        assert data.getAsJsonObject("criteria").size()==9 && data.getAsJsonArray("requirements").size()==9;
        for(var row:data.getAsJsonArray("requirements"))assert row.getAsJsonArray().size()==1;
        for(int dex:new int[]{25,172,383,890})capture(dex,false);
        assert captured.isEmpty() && !complete():"Unrelated species awarded family criteria";
        for(int i=0;i<12;i++)capture(133,false);
        assert captured.size()==1 && !complete():"Repeating Eevee completed family";
        for(int dex:new int[]{134,135,136,196,197,470,471})capture(dex,false);
        assert captured.size()==8 && !complete():"Incomplete family completed";
        capture(700,true);assert !complete():"Egg completed family";
        capture(700,false);assert captured.size()==9 && complete();
        System.out.println("PASS family: unrelated appearances, duplicates, eight members and eggs fail; all nine distinct species complete native AND requirements");
    }
}
