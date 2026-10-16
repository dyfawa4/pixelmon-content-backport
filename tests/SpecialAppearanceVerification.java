import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import javax.tools.ToolProvider;
import com.google.gson.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Execute production criterion guards and the native matcher fallback with an unresolved-spec fixture. */
public final class SpecialAppearanceVerification {
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    static void add(Map<String,String> sources,String name,String body){sources.put(name,"package "+name.substring(0,name.lastIndexOf('.'))+";"+body);}
    public static void main(String[] args)throws Exception{
        Map<String,Integer> dex=new LinkedHashMap<>();
        String[] names={"feebas","magikarp","shellos","clobbopus","pichu","onix","bidoof","spheal","eevee","vaporeon","jolteon","flareon","espeon","umbreon","leafeon","glaceon","sylveon","unown","minior"};
        int[] ids={349,129,422,852,172,95,399,363,133,134,135,136,196,197,470,471,700,201,774};
        for(int i=0;i<names.length;i++)dex.put(names[i],ids[i]);
        JsonArray guarded=new JsonArray();int resources=0,total=0;
        MethodNode nativeTest=null;
        try(var jar=new JarFile(args[0])){
            var entries=jar.entries();
            while(entries.hasMoreElements()){
                var entry=entries.nextElement();String name=entry.getName();
                if(!name.startsWith("data/pixelmon/advancements/specs/")||!name.endsWith(".json"))continue;
                JsonObject original;
                try(var in=jar.getInputStream(entry)){original=new JsonParser().parse(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
                var stream=SpecialAppearanceVerification.class.getClassLoader().getResourceAsStream(name);
                if(stream==null)continue;
                JsonObject patched;try(var in=stream){patched=new JsonParser().parse(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
                // These existing overrides intentionally replace the disguised-capture trigger / Eevee AND requirements.
                boolean existing=name.endsWith("/feebas_karp_trigger.json")||name.endsWith("/eeveelution_capture_all.json");
                JsonObject withoutMetadata=new JsonParser().parse(patched.toString()).getAsJsonObject();int count=0;
                for(var row:patched.getAsJsonObject("criteria").entrySet()){
                    JsonObject criterion=row.getValue().getAsJsonObject();
                    if(!criterion.get("trigger").getAsString().equals("pixelmon:spec_trigger"))continue;
                    JsonObject conditions=criterion.getAsJsonObject("conditions");String spec=conditions.get("spec").getAsString();
                    String first=spec.split("\\s+")[0].toLowerCase(Locale.ROOT);
                    if(dex.containsKey(first)){
                        check(conditions.has("backport_expected_dex")&&conditions.get("backport_expected_dex").getAsInt()==dex.get(first),"Missing/wrong exact species: "+name+"/"+row.getKey());
                        JsonObject test=new JsonParser().parse(conditions.toString()).getAsJsonObject();test.addProperty("criterion",name+"/"+row.getKey());guarded.add(test);count++;
                    }else check(!conditions.has("backport_expected_dex"),"Global palette/shiny condition gained a species restriction");
                    withoutMetadata.getAsJsonObject("criteria").getAsJsonObject(row.getKey()).getAsJsonObject("conditions").remove("backport_expected_dex");
                }
                if(!existing)check(original.equals(withoutMetadata),"Native display, palette/form/shiny, criteria IDs or requirements changed: "+name);
                if(count>0){resources++;total+=count;}
            }
            var node=new ClassNode();try(var in=jar.getInputStream(jar.getJarEntry("com/pixelmonmod/pixelmon/api/advancements/triggers/SpecTrigger$Instance.class"))){new ClassReader(in.readAllBytes()).accept(node,0);}
            for(var method:node.methods)if(method.name.equals("test"))nativeTest=method;
        }
        check(resources==15&&total==250,"Appearance resource coverage drift: "+resources+"/"+total);
        check(nativeTest!=null,"Missing native matcher fallback");
        var root=Version13StaticVerification.resource("data/pixelmon/advancements/specs/spec_trigger.json");
        for(String key:new String[]{"eeveelution_capture_all","unown_capture_all","minior_capture_all"})check(root.getAsJsonObject("criteria").getAsJsonObject(key).getAsJsonObject("conditions").has("backport_expected_dex"),"Reported start-of-game criterion unguarded");
        check(root.getAsJsonArray("requirements").size()==1&&root.getAsJsonArray("requirements").get(0).getAsJsonArray().size()==79,"Native special-appearance OR requirements changed");
        Map<String,String> sources=new LinkedHashMap<>();String api="com.pixelmonmod.pixelmon.api.";
        add(sources,api+"pokemon.species.Species","public class Species {public int dex;public Species(int d){dex=d;}public int getDex(){return dex;}}");
        add(sources,api+"pokemon.Pokemon","public class Pokemon {public boolean egg;public "+api+"pokemon.species.Species species;public Pokemon(int d,boolean e){species=new "+api+"pokemon.species.Species(d);egg=e;}public boolean isEgg(){return egg;}public "+api+"pokemon.species.Species getSpecies(){return species;}}");
        add(sources,"com.pixelmonmod.api.pokemon.PokemonSpecification","public class PokemonSpecification {public boolean result;public PokemonSpecification(boolean r){result=r;}public boolean matches(Object p){return result;}}");
        add(sources,"net.minecraft.loot.ConditionArrayParser","public class ConditionArrayParser {}");
        add(sources,api+"advancements.triggers.SpecTrigger","public class SpecTrigger {public static class Instance extends local.pixelmon.backport.mixin.SpecCriterionSpeciesMixin {public com.pixelmonmod.api.pokemon.PokemonSpecification spec;public Instance(boolean r){spec=new com.pixelmonmod.api.pokemon.PokemonSpecification(r);}public boolean test("+api+"pokemon.Pokemon p){throw new AssertionError();}}}");
        try(var in=SpecialAppearanceVerification.class.getClassLoader().getResourceAsStream("SpecialAppearanceCases.java.txt")){sources.put("fixture.SpecialAppearanceCases",new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}
        Path dir=Files.createTempDirectory("appearance-criterion-");List<String> compiler=new ArrayList<>(List.of("--release","11","-proc:none","-classpath",System.getProperty("java.class.path"),"-d",dir.toString()));
        for(var source:sources.entrySet()){Path path=dir.resolve(source.getKey().replace('.','/')+".java");Files.createDirectories(path.getParent());Files.writeString(path,source.getValue());compiler.add(path.toString());}
        check(ToolProvider.getSystemJavaCompiler().run(null,null,null,compiler.toArray(new String[0]))==0,"Fixture compilation failed");
        Path target=dir.resolve((api+"advancements.triggers.SpecTrigger$Instance").replace('.','/')+".class");var stub=new ClassNode();new ClassReader(Files.readAllBytes(target)).accept(stub,0);stub.methods.removeIf(m->m.name.equals("test"));stub.methods.add(nativeTest);var writer=new ClassWriter(0);stub.accept(writer);Files.write(target,writer.toByteArray());
        try(var loader=new URLClassLoader(new URL[]{dir.toUri().toURL()},SpecialAppearanceVerification.class.getClassLoader()){
            protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{synchronized(getClassLoadingLock(n)){Class<?> c=findLoadedClass(n);String base=n.contains("$")?n.substring(0,n.indexOf('$')):n;if(c==null&&sources.containsKey(base))c=findClass(n);if(c==null&&n.startsWith("local.pixelmon.backport.")&&(n.endsWith("SpecCriterionFactoryMixin")||n.endsWith("SpecCriterionSpeciesMixin")||n.endsWith("SpeciesCriterionGuard")||n.endsWith("SpeciesCriterionPolicy"))){try(var in=getParent().getResourceAsStream(n.replace('.','/')+".class")){byte[] b=in.readAllBytes();c=defineClass(n,b,0,b.length);}catch(java.io.IOException ex){throw new ClassNotFoundException(n,ex);}}if(c==null)c=super.loadClass(n,false);if(resolve)resolveClass(c);return c;}}
        }){loader.loadClass("fixture.SpecialAppearanceCases").getMethod("run",String.class).invoke(null,guarded.toString());}
        System.out.println("PASS 250 exact-species appearance criteria across 15 resources; actual factory/test guard hooks plus native Instance.test fallback; starter misaward, all wrong species, eggs/null, palette/form rejection, preserved OR/AND and shiny/global conditions. No game initialized.");
    }
}
