import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.pokemon.ability.AbilityRegistry;
import com.pixelmonmod.pixelmon.api.battles.attack.AttackRegistry;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import net.minecraft.util.ResourceLocation;
import local.pixelmon.backport.*;

/** Native Gson/registries, plus checks that every untouched legacy entry stays
 * byte-equivalent. This is not a substitute for in-game capture/animation QA. */
public final class NativeDataVerification {
    public static void main(String[] args)throws Exception {
        if(!System.getProperty("java.specification.version").equals("11"))throw new AssertionError("Use Java 11");
        Map<ResourceLocation,JsonElement> species=new LinkedHashMap<>(),moves=new LinkedHashMap<>(),abilities=new LinkedHashMap<>();
        JsonParser parser=new JsonParser();
        try(JarFile jar=new JarFile(args[0])) {
            Enumeration<JarEntry> entries=jar.entries();
            while(entries.hasMoreElements()){
                JarEntry entry=entries.nextElement();String path=entry.getName();
                if(!path.endsWith(".json"))continue;
                String type=path.startsWith("data/pixelmon/species/")?"species":path.startsWith("data/pixelmon/moves/")?"moves":path.startsWith("data/pixelmon/abilities/")?"abilities":null;
                if(type==null)continue;
                JsonElement value;
                try(InputStream stream=jar.getInputStream(entry)){value=parser.parse(new InputStreamReader(stream,StandardCharsets.UTF_8));}
                ResourceLocation key=new ResourceLocation("pixelmon",path.substring(("data/pixelmon/"+type+"/").length(),path.length()-5));
                (type.equals("species")?species:type.equals("moves")?moves:abilities).put(key,value);
            }
        }
        Map<ResourceLocation,String> before=new HashMap<>();species.forEach((k,v)->before.put(k,v.toString()));
        Content.apply("pixelmon","species",species);Content.apply("pixelmon","moves",moves);Content.apply("pixelmon","abilities",abilities);
        for(JsonElement row:species.values())if(Arrays.asList(1026,1029,1032).contains(row.getAsJsonObject().get("dex").getAsInt()))throw new AssertionError("Incomplete preview starter enabled");
        if(Content.DATA.getAsJsonArray("starterAdditions").size()!=0)throw new AssertionError("Incomplete starter selection enabled");
        Set<Integer> changed=new HashSet<>();
        for(JsonElement patch:Content.DATA.getAsJsonArray("patches"))changed.add(patch.getAsJsonObject().getAsJsonObject("species").get("dex").getAsInt());
        changed.add(658);
        for(Map.Entry<ResourceLocation,JsonElement> entry:species.entrySet())if(entry.getValue().getAsJsonObject().get("dex").getAsInt()==658){
            JsonObject current=parser.parse(entry.getValue().toString()).getAsJsonObject();
            JsonObject original=parser.parse(before.get(entry.getKey())).getAsJsonObject();
            for(JsonElement form:original.getAsJsonArray("forms")){
                String name=form.getAsJsonObject().get("name").getAsString();if(!name.equals("ash") && !name.equals("battle_bond"))continue;
                JsonElement present=null;for(JsonElement f:current.getAsJsonArray("forms"))if(f.getAsJsonObject().get("name").getAsString().equals(name))present=f;
                if(!form.equals(present))throw new AssertionError("Changed original Greninja form "+name);
            }
        }
        int unchanged=0;
        for(Map.Entry<ResourceLocation,JsonElement> entry:species.entrySet()) {
            int dex=entry.getValue().getAsJsonObject().get("dex").getAsInt();
            if(!changed.contains(dex)) {
                JsonObject actual=parser.parse(entry.getValue().toString()).getAsJsonObject();
                JsonObject original=parser.parse(before.get(entry.getKey())).getAsJsonObject();
                // Permit only the explicitly advertised native Tera Blast learn rule.
                for(int i=0;i<actual.getAsJsonArray("forms").size();i++){
                    JsonObject a=actual.getAsJsonArray("forms").get(i).getAsJsonObject(),o=original.getAsJsonArray("forms").get(i).getAsJsonObject();
                    if(!a.has("moves"))continue;
                    JsonObject inherited=original.getAsJsonArray("forms").get(0).getAsJsonObject();
                    if(o.has("defaultBaseForm") && !o.get("defaultBaseForm").getAsString().isEmpty())for(JsonElement candidate:original.getAsJsonArray("forms"))
                        if(o.get("defaultBaseForm").equals(candidate.getAsJsonObject().get("name")))inherited=candidate.getAsJsonObject();
                    JsonObject m=a.getAsJsonObject("moves"),om=o.has("moves")?o.getAsJsonObject("moves"):inherited.has("moves")?inherited.getAsJsonObject("moves"):new JsonObject();
                    if(m.has("tmMoves9") && (!om.has("tmMoves9") || !om.getAsJsonArray("tmMoves9").contains(new JsonPrimitive("Tera Blast")))){m.getAsJsonArray("tmMoves9").remove(new JsonPrimitive("Tera Blast"));if(!om.has("tmMoves9") && m.getAsJsonArray("tmMoves9").size()==0)m.remove("tmMoves9");}
                    if(m.has("levelUpMoves")){JsonArray levels=m.getAsJsonArray("levelUpMoves");for(int j=levels.size()-1;j>=0;j--){JsonObject level=levels.get(j).getAsJsonObject();if(level.get("level").getAsInt()!=50)continue;boolean originalBlast=false;if(om.has("levelUpMoves"))for(JsonElement old:om.getAsJsonArray("levelUpMoves"))if(old.getAsJsonObject().get("level").getAsInt()==50 && old.getAsJsonObject().getAsJsonArray("attacks").contains(new JsonPrimitive("Tera Blast")))originalBlast=true;if(!originalBlast)level.getAsJsonArray("attacks").remove(new JsonPrimitive("Tera Blast"));if(level.getAsJsonArray("attacks").size()==0)levels.remove(j);}}
                    if(!o.has("moves") && m.equals(om))a.remove("moves");
                }
                if(!actual.equals(original))throw new AssertionError("Changed legacy entry outside Tera Blast rule "+dex);
                ++unchanged;
            }
        }
        String once=species.toString();Content.apply("pixelmon","species",species);
        if(!once.equals(species.toString()))throw new AssertionError("Not idempotent");
        // Called inside the real Forge test server: item/sound registries and
        // native Pixelmon datapacks have already finished loading.
        if(!Boolean.getBoolean("backport.test.live"))throw new IllegalStateException("Run through live-check.ps1 in Forge, not a bare JVM");
        int parsed=0;
        for(JsonElement value:species.values())if(changed.contains(value.getAsJsonObject().get("dex").getAsInt())) {
            Species s=Species.GSON.fromJson(value,Species.class);
            if(s.getForms().isEmpty())throw new AssertionError("No forms "+s.getName());
            ++parsed;
            System.out.println("PARSED "+s.getDex()+" "+s.getName()+" forms="+s.getForms().size());
        }
        System.out.println("PASS native data: "+parsed+" patched species; "+unchanged+" legacy species unchanged; reload idempotent");
    }
}
