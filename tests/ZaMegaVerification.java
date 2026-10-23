import com.google.gson.*;
import local.pixelmon.backport.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import javax.imageio.ImageIO;

/** Final merged forms, native Boss routes, icon references and Ash origin. */
public final class ZaMegaVerification {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static JsonObject form(JsonObject s,String n){for(JsonElement row:s.getAsJsonArray("forms"))if(row.getAsJsonObject().get("name").getAsString().equals(n))return row.getAsJsonObject();throw new AssertionError("Missing form "+s.get("name")+"/"+n);}
    public static void main(String[] args)throws Exception{
        Map<Integer,JsonObject> all=new TreeMap<>();
        try(JarFile jar=new JarFile(args[0])){for(var entries=jar.entries();entries.hasMoreElements();){var e=entries.nextElement();if(!e.getName().startsWith("data/pixelmon/species/")||!e.getName().endsWith(".json"))continue;try(Reader in=new InputStreamReader(jar.getInputStream(e),StandardCharsets.UTF_8)){JsonObject s=new JsonParser().parse(in).getAsJsonObject();all.put(s.get("dex").getAsInt(),s);}}}
        for(JsonElement row:Content.DATA.getAsJsonArray("patches")){JsonObject s=row.getAsJsonObject().getAsJsonObject("species");int dex=s.get("dex").getAsInt();all.put(dex,all.containsKey(dex)?Content.mergeSpecies(all.get(dex),s):s.deepCopy());}
        for(JsonObject s:all.values()){ZaMegaData.normalize(s);String once=s.toString();ZaMegaData.normalize(s);check(once.equals(s.toString()),"Non-idempotent Mega normalization #"+s.get("dex"));}
        JsonObject official=Content.read("backport/za-mega-abilities.json");int abilityCount=0;
        for(var species:official.entrySet())for(var a:species.getValue().getAsJsonObject().entrySet())if(!a.getValue().isJsonNull()){
            JsonObject f=form(all.get(Integer.parseInt(species.getKey())),a.getKey());check(f.getAsJsonObject("abilities").getAsJsonArray("abilities").size()==1&&f.getAsJsonObject("abilities").getAsJsonArray("abilities").get(0).equals(a.getValue()),"Official ability mismatch "+species.getKey()+"/"+a.getKey());abilityCount++;
        }
        Set<String> spawnIds=new HashSet<>();int count=0;
        for(JsonElement row:Content.read("backport/za-mega-import.json").getAsJsonArray("forms")){
            JsonObject r=row.getAsJsonObject(),s=all.get(r.get("dex").getAsInt()),f=form(s,r.get("form").getAsString());String item=Content.NS+":"+r.get("stone").getAsString(),name=f.get("name").getAsString();
            check(f.getAsJsonArray("megaItems").size()==1&&f.getAsJsonArray("megaItems").get(0).getAsString().equals(item),"Sibling Boss stone leak "+s.get("name"));
            check(f.getAsJsonArray("megas").get(0).getAsString().equals(name),"Boss form rerolled "+s.get("name"));
            for(JsonElement g:f.getAsJsonArray("genderProperties"))for(JsonElement palette:g.getAsJsonObject().getAsJsonArray("palettes")){
                String ref=palette.getAsJsonObject().get("sprite").getAsString();String[] p=ref.split(":",2);Path icon=Path.of("resources/assets",p[0],"textures",p[1]);check(Files.isRegularFile(icon),"Missing Boss portrait "+ref);
                var im=ImageIO.read(icon.toFile());check(im.getWidth()==32&&im.getHeight()==32&&im.getColorModel().hasAlpha(),"Non-native Boss portrait "+ref);
            }
            JsonObject spawn=Content.read("data/pixelmon/spawning/megas/backport_"+s.get("name").getAsString().toLowerCase(Locale.ROOT)+"_"+name+".set.json");check(spawnIds.add(spawn.get("id").getAsString()),"Duplicate Boss spawn id");
            for(JsonElement info:spawn.getAsJsonArray("spawnInfos")){JsonObject i=info.getAsJsonObject();check(i.get("spec").getAsString().contains("form:"+name),"Ambiguous Boss form "+s.get("name"));check(i.get("spawnSpecificBossRate").getAsDouble()==1,"Catchable ordinary spawn "+s.get("name"));}
            count++;
        }
        for(int dex:new int[]{359,445,448}){
            JsonObject s=all.get(dex),old=form(s,"mega"),z=form(s,"megaz");check(!old.getAsJsonArray("megaItems").equals(z.getAsJsonArray("megaItems")),"Old/Z stone collision #"+dex);check(old.getAsJsonArray("megaItems").size()==1,"Old Mega gained Z stone #"+dex);
        }
        for(int dex:new int[]{801,978}){
            JsonObject s=all.get(dex);String[] origins=dex==801?new String[]{"","original_color"}:new String[]{"curly","droopy","stretchy"};String[] targets=dex==801?new String[]{"mega","megaoriginal"}:new String[]{"mega","megadroopy","megastretchy"};Set<String> models=new HashSet<>();Set<String> palettes=new HashSet<>();
            for(int i=0;i<origins.length;i++){
                JsonObject base=form(s,origins[i]),f=form(s,targets[i]);check(base.getAsJsonArray("megas").size()==1&&base.getAsJsonArray("megas").get(0).getAsString().equals(targets[i]),"Variant route lost #"+dex+"/"+origins[i]);
                check(f.getAsJsonArray("megas").get(0).getAsString().equals(targets[i])&&f.get("defaultBaseForm").getAsString().equals(origins[i]),"Variant Boss / original form lost");
                for(JsonElement g:f.getAsJsonArray("genderProperties"))for(JsonElement p:g.getAsJsonObject().getAsJsonArray("palettes")){
                    JsonObject palette=p.getAsJsonObject();models.add(palette.get("modelLocator").toString());palettes.add(palette.get("texture").getAsString());check(!palette.get("emissive").getAsString().isEmpty(),"Invalid variant emissive path");
                    for(String key:new String[]{"texture","sprite"}){String[] ref=palette.get(key).getAsString().split(":",2);check(Files.isRegularFile(Path.of("resources/assets",ref[0],"textures",ref[1])),"Native texture prefix mismatch "+palette.get(key));}
                    for(JsonElement resource:palette.getAsJsonObject("modelLocator").getAsJsonArray("pqc")){String[] ref=resource.getAsString().split(":",2);check(Files.isRegularFile(Path.of("resources/assets",ref[0],"textures",ref[1])),"Native PQC prefix mismatch "+resource);}
                }
                if(i>0){JsonObject spawn=Content.read("data/pixelmon/spawning/megas/backport_"+s.get("name").getAsString().toLowerCase(Locale.ROOT)+"_"+targets[i]+".set.json");check(spawnIds.add(spawn.get("id").getAsString()),"Duplicate variant Boss id");for(JsonElement info:spawn.getAsJsonArray("spawnInfos")){JsonObject entry=info.getAsJsonObject();check(entry.get("spec").getAsString().contains("form:"+targets[i])&&entry.get("spawnSpecificBossRate").getAsDouble()==1,"Catchable / wrong variant Boss");}}
            }
            check(models.size()==3&&palettes.size()==2,"Missing smooth variants or original ordinary/shiny textures #"+dex);
            for(String model:models)check((model.contains("reference/pokemon/"+dex+"_")||model.contains("smooth_variants/"+dex+"_"))&&!model.contains("za_variants/"),"Rejected block model restored #"+dex);
        }
        for(JsonElement row:Content.read("backport/za-variant-assets.json").getAsJsonArray("models"))for(JsonElement file:row.getAsJsonObject().getAsJsonArray("files")){
            JsonObject f=file.getAsJsonObject();byte[] bytes=Files.readAllBytes(Path.of("resources",f.get("resource").getAsString()));
            StringBuilder hash=new StringBuilder();for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(bytes))hash.append(String.format("%02x",b&255));
            check(hash.toString().equals(f.get("sha256").getAsString()),"Original visual changed "+f.get("resource"));
        }
        JsonObject greninja=all.get(658);Map<net.minecraft.util.ResourceLocation,JsonElement> one=new LinkedHashMap<>();var id=new net.minecraft.util.ResourceLocation("pixelmon","greninja");one.put(id,greninja);PermanentAsh.addForm(one);greninja=one.get(id).getAsJsonObject();Content.addNinjaLearning(greninja);Content.applyTeraLearning(greninja);
        JsonObject mega=form(greninja,"mega"),ash=form(greninja,PermanentAsh.MEGA);
        for(JsonObject f:List.of(mega,ash))for(JsonElement g:f.getAsJsonArray("genderProperties"))for(JsonElement palette:g.getAsJsonObject().getAsJsonArray("palettes"))check("pixelmon:empty.png".equals(palette.getAsJsonObject().get("emissive").getAsString()),"Empty or inherited Mega Greninja emissive texture");
        try(JarFile nativeJar=new JarFile(args[0])){check(nativeJar.getJarEntry("assets/pixelmon/textures/empty.png")!=null,"Missing native transparent texture");}
        check(mega.get("battleStats").equals(ash.get("battleStats"))&&ash.getAsJsonObject("abilities").getAsJsonArray("abilities").get(0).getAsString().equals("Drizzle"),"Ash Mega has fake stats/ability");
        for(String n:new String[]{"ash",PermanentAsh.FORM})check(form(greninja,n).getAsJsonArray("megas").size()==0,"Ash Mega eligibility lost "+n);
        check(ash.getAsJsonObject("moves").getAsJsonArray("tutorMoves").contains(new JsonPrimitive("Ninja Training")),"Ash Mega lost training");
        check(ash.getAsJsonObject("moves").getAsJsonArray("tmMoves9").contains(new JsonPrimitive("Tera Blast")),"Ash Mega lost Tera Blast");
        all.put(658,greninja);
        JsonObject gallery=new JsonParser().parse(Files.readString(Path.of("test-maps/mega-resources/mega-gallery.json"))).getAsJsonObject();Set<String> exhibits=new HashSet<>();
        for(JsonElement row:gallery.getAsJsonArray("entries")){
            JsonObject r=row.getAsJsonObject();int dex=r.get("dex").getAsInt();String n=r.get("form").getAsString(),gender=r.has("gender")?r.get("gender").getAsString():"",palette=r.get("palette").getAsString();
            JsonObject f=form(all.get(dex),n);check(exhibits.add(dex+"/"+n+"/"+gender+"/"+palette),"Duplicate gallery subject");
            if(r.get("boss").getAsBoolean())check(n.startsWith("mega"),"Non-Mega marked as Boss in gallery");
            check(!r.get("label").getAsString().isBlank(),"Missing gallery label");
        }
        check(exhibits.contains("801/original_color//none"),"Original-color non-Mega missing from test map");
        for(String n:List.of("curly","droopy","stretchy"))check(exhibits.contains("978/"+n+"//none"),"Tatsugiri base missing from gallery");
        JsonObject catalogue=new JsonParser().parse(Files.readString(Path.of("docs/ZA_MEGA_COVERAGE_1.6.0.json"))).getAsJsonObject();
        for(JsonElement row:catalogue.getAsJsonArray("forms")){
            JsonObject r=row.getAsJsonObject();int dex=r.get("dex").getAsInt();String n=r.has("runtime_form")?r.get("runtime_form").getAsString():r.get("form").getAsString(),gender="";
            if(dex==678){gender=n.equals("fmega")?"FEMALE":"MALE";n="mega";}
            check(exhibits.contains(dex+"/"+n+"/"+gender+"/none"),"ZA catalogue absent from gallery #"+dex+"/"+n);
        }
        System.out.println("PASS gallery: "+exhibits.size()+" unique subjects; complete ZA catalogue, both Meowstic genders, base original-color Magearna, all Tatsugiri origins and shiny comparisons.");
        String beforeReload=greninja.toString();ZaMegaData.normalize(greninja);one.put(id,greninja);PermanentAsh.addForm(one);Content.addNinjaLearning(one.get(id).getAsJsonObject());Content.applyTeraLearning(one.get(id).getAsJsonObject());check(beforeReload.equals(one.get(id).toString()),"Ash Mega origin changes on data reload");
        System.out.println("PASS ZA merged Mega forms="+count+", official ability mappings="+abilityCount+", native portraits, exclusive Boss stones, old/Z separation and ability-independent Ash Mega learning.");
    }
}
