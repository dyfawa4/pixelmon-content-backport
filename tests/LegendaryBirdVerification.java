import local.pixelmon.backport.LegendaryCriterionPolicy;
import com.google.gson.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
public final class LegendaryBirdVerification {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        JsonObject forms=Version13StaticVerification.resource("backport/advancements.json").getAsJsonObject("capturesByForm");
        try(ZipFile jar=new ZipFile(args[0])){
            String[] names={"articuno","zapdos","moltres"};
            for(int i=0;i<3;i++){
                String name=names[i];int dex=144+i;
                try(InputStream in=jar.getInputStream(jar.getEntry("data/pixelmon/advancements/legendaries/"+name+"_capture_trigger.json"))){
                    JsonObject nativeAdv=new JsonParser().parse(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                    JsonObject criterion=nativeAdv.getAsJsonObject("criteria").getAsJsonObject(name+"_capture");
                    check(criterion.get("trigger").getAsString().equals("pixelmon:legendary_capture_trigger") && criterion.getAsJsonObject("conditions").get("pokemon").getAsString().equals(name),"Native trigger not matched");
                }
                check(LegendaryCriterionPolicy.accepts(name,dex,"",false),"Ordinary bird blocked");
                check(LegendaryCriterionPolicy.accepts(name.toUpperCase(Locale.ROOT),dex,"base",false),"Base alias blocked");
                check(!LegendaryCriterionPolicy.accepts(name,dex,"galarian",false),"Galarian also earns ordinary");
                check(!LegendaryCriterionPolicy.accepts(name,dex,"",true),"Egg earns ordinary");
                check(!LegendaryCriterionPolicy.accepts(name,150,"",false),"Wrong species accepted");
                check(LegendaryCriterionPolicy.accepts("legendary",dex,"galarian",false),"Generic first-legend root blocked");
                check(forms.getAsJsonObject(Integer.toString(dex)).size()==1 && forms.getAsJsonObject(Integer.toString(dex)).has("galarian"),"Galarian specific award lost");
                Set<String> awarded=new HashSet<>();for(String form:new String[]{"galarian","","galarian",""}){if(LegendaryCriterionPolicy.accepts(name,dex,form,false))awarded.add("ordinary");if(form.equals("galarian"))awarded.add("galarian");}check(awarded.size()==2,"Order/duplicate bug");
            }
        }
        check(LegendaryCriterionPolicy.accepts("mewtwo",150,"megax",false),"Unrelated native form semantics changed");
        System.out.println("PASS all three native ordinary bird criteria, Galarian-only award routing, eggs/wrong species/aliases, acquisition order/duplicate boundaries and unchanged generic legendary root; no game initialized.");
    }
}
