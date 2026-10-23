import local.pixelmon.backport.GymAcePolicy;
import com.google.gson.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class GymAceVerification {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        for(int dex:new int[]{789,790,772,891,803,808})check(!GymAcePolicy.strong(dex,720,true,true),"Immature legend admitted as ace "+dex);
        check(!GymAcePolicy.strong(898,500,true,true),"Weak base form passes on legendary label alone");
        check(GymAcePolicy.strong(892,550,true,true) && !GymAcePolicy.strong(892,549,true,true),"Urshifu mature competitive exception");
        check(!GymAcePolicy.eligible(150,680,true,true,false),"Unrelated Mewtwo admitted into Dark theme");
        check(!GymAcePolicy.eligible(248,600,false,true,true),"Pseudo legendary confused with required legendary ace");
        JsonObject plan=GymAcePolicy.DATA;check(plan.size()==18,"Missing theme ace plan");int candidates=0;
        try(ZipFile jar=new ZipFile(args[0])){
            for(var entry:plan.entrySet())for(JsonElement value:entry.getValue().getAsJsonArray()){
                var row=value.getAsJsonObject();int dex=row.get("dex").getAsInt();String prefix=String.format("data/pixelmon/species/%03d_",dex);ZipEntry nativeEntry=null;
                for(var all=jar.entries();all.hasMoreElements();){var e=all.nextElement();if(e.getName().startsWith(prefix) && e.getName().endsWith(".json")){nativeEntry=e;break;}}
                JsonObject species;
                if(nativeEntry!=null)try(var in=jar.getInputStream(nativeEntry)){species=new JsonParser().parse(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
                else{
                    File[] files=new File("resources/data/pixelmon/species").listFiles((dir,name)->name.startsWith(String.format("%03d_",dex)) && name.endsWith(".json"));
                    if(files!=null && files.length==1){try(var reader=new FileReader(files[0])){species=new JsonParser().parse(reader).getAsJsonObject();}}
                    else {species=null;JsonObject content=JsonParser.parseString(java.nio.file.Files.readString(java.nio.file.Path.of("resources/backport/content.json"))).getAsJsonObject();for(var patch:content.getAsJsonArray("patches")){var candidate=patch.getAsJsonObject().getAsJsonObject("species");if(candidate.get("dex").getAsInt()==dex)species=candidate;}check(species!=null,"Unresolved backport ace species "+dex);}
                }
                JsonObject base=species.getAsJsonArray("forms").get(0).getAsJsonObject(),form=base;
                if(row.has("form")){form=null;for(var f:species.getAsJsonArray("forms"))if(row.get("form").getAsString().equals(f.getAsJsonObject().get("name").getAsString()))form=f.getAsJsonObject();}
                check(form!=null,"Unavailable battle form "+row);int total=0;for(var stat:form.getAsJsonObject("battleStats").entrySet())total+=stat.getValue().getAsInt();
                boolean tagged=base.getAsJsonArray("tags").contains(new JsonPrimitive("legendary")) || base.getAsJsonArray("tags").contains(new JsonPrimitive("mythical"));
                boolean matching=form.getAsJsonArray("types").contains(new JsonPrimitive(entry.getKey().toUpperCase(Locale.ROOT)));
                check(GymAcePolicy.eligible(dex,total,tagged,true,matching),"Weak/unrelated/unregistered candidate "+entry.getKey()+" "+row);
                if(dex==892){String pool=form.getAsJsonObject("moves").toString();String signature=row.get("form").getAsString().equals("rapidstrike")?"Surging Strikes":"Wicked Blow";
                    for(String move:new String[]{signature,"Close Combat","U-turn",signature.equals("Surging Strikes")?"Aqua Jet":"Sucker Punch"})check(pool.contains("\""+move+"\""),"Illegal Urshifu plan "+move);
                }
                candidates++;
            }
        }
        String source=java.nio.file.Files.readString(java.nio.file.Path.of("src/local/pixelmon/backport/NpcGimmicks.java"));
        check(!source.contains("random.nextInt(5)==0") && !source.contains("candidates=all"),"Cross-theme fallback retained");
        check(source.contains("replaceLegacyCrossTheme?participant.allPokemon.length-1") && source.contains("!last.getForm().getTypes().contains(theme)"),"Old generated cross-theme ace retained as an unrelated gym member");
        check(source.contains("aceEligible(member.pokemon,theme)") && source.contains("BackportNpcRosterVersion"),"Old-save presence flag still bypasses mature ace validation");
        System.out.println("PASS all 18 curated theme plans / "+candidates+" native-resource candidates, mature strength/ordinary weak legends and pseudo-legend negatives, both Urshifu form legal signatures, old-save revalidation and removed cross-theme fallback; no game initialized.");
    }
}
