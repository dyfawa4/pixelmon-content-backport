import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import com.google.gson.*;
import net.minecraft.util.ResourceLocation;
import local.pixelmon.backport.Content;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Cross-check actual native structure naming and reference lookup without a game. */
public final class CapSpawnVerification {
    static ClassNode node(InputStream in)throws IOException{
        try(in){ClassNode n=new ClassNode();new ClassReader(in.readAllBytes()).accept(n,0);return n;}
    }
    static ClassNode resource(String name)throws IOException{return node(CapSpawnVerification.class.getClassLoader().getResourceAsStream(name+".class"));}
    static ClassNode nativeClass(JarFile jar,String name)throws IOException{return node(jar.getInputStream(jar.getJarEntry(name+".class")));}
    static MethodNode method(ClassNode n,String name){return n.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
    static boolean call(MethodNode m,String owner,String name){
        for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof MethodInsnNode){MethodInsnNode c=(MethodInsnNode)i;if(c.owner.equals(owner)&&c.name.equals(name))return true;}return false;
    }
    static boolean constant(MethodNode m,Object value){for(AbstractInsnNode i:m.instructions.toArray()){
        if(i instanceof LdcInsnNode&&value.equals(((LdcInsnNode)i).cst))return true;
        if(i instanceof IntInsnNode&&value.equals(((IntInsnNode)i).operand))return true;
    }return false;}
    static boolean field(MethodNode m,String owner,String name){for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof FieldInsnNode){FieldInsnNode f=(FieldInsnNode)i;if(f.owner.equals(owner)&&f.name.equals(name))return true;}return false;}
    static void require(boolean b,String reason){if(!b)throw new AssertionError(reason);}
    public static void main(String[] args)throws Exception{
        verifyMagearnaRules(args[0]);
        String structure="net/minecraft/world/gen/feature/structure/Structure";
        ClassNode mc=resource(structure);MethodNode registrations=method(mc,"<clinit>");
        require(constant(registrations,"Village")&&!constant(registrations,"minecraft:village"),"Actual native structure registration differs");
        MethodNode register=method(mc,"func_236394_a_");
        require(field(register,structure,"field_236365_a_")&&call(register,"java/lang/String","toLowerCase"),"Native legacy names no longer lowercase registration keys");
        try(JarFile jar=new JarFile(args[0])){
            ClassNode helper=nativeClass(jar,"com/pixelmonmod/pixelmon/api/util/helpers/WorldHelper");
            MethodNode resolver=method(helper,"lambda$insideStructure$0");
            require(field(resolver,structure,"field_236365_a_")&&call(resolver,"com/google/common/collect/BiMap","get"),"Wrong-key root cause changed");
            MethodNode cache=method(helper,"onLoad");
            require(call(cache,"net/minecraft/world/chunk/IChunk","func_201609_c"),"Native cache now reads cross-chunk references; review fix");
            ClassNode check=nativeClass(jar,"com/pixelmonmod/pixelmon/api/spawning/archetypes/algorithms/checkspawns/LegendaryCheckSpawns");
            require(call(method(check,"checkSpawns"),"com/pixelmonmod/pixelmon/api/spawning/AbstractSpawner","getSuitableSpawns"),"Native query no longer shares spawn predicates");
        }
        JsonObject row;
        try(Reader in=new InputStreamReader(CapSpawnVerification.class.getClassLoader().getResourceAsStream("data/pixelmon/spawning/legendaries/cappikachu.set.json"),java.nio.charset.StandardCharsets.UTF_8)){
            row=new JsonParser().parse(in).getAsJsonObject().getAsJsonArray("spawnInfos").get(0).getAsJsonObject();
        }
        JsonObject condition=row.getAsJsonObject("condition");
        require(condition.getAsJsonArray("structures").toString().equals("[\"village\"]"),"Unsupported structure ID blocks actual spawn/query");
        require(condition.getAsJsonArray("stringBiomes").toString().equals("[\"minecraft:plains\"]")&&row.get("spec").getAsString().equals("species:CapPikachu level:70")&&row.get("interval").getAsString().equals("legendary"),"Species, plains/level/timer constraints changed");
        ClassNode guard=resource("local/pixelmon/backport/CapSpawns");MethodNode village=method(guard,"village"),spawn=method(guard,"spawn");
        require(call(village,"net/minecraft/world/server/ServerWorld","func_241112_a_")&&field(village,structure,"field_236381_q_")&&call(village,"net/minecraft/world/gen/feature/structure/StructureManager","func_235010_a_")&&call(village,"net/minecraft/world/gen/feature/structure/StructureStart","func_75069_d"),"Real native village reference/validity lookup missing");
        require(call(spawn,"local/pixelmon/backport/CapSpawns","eligible")&&constant(method(guard,"eligible"),"minecraft:plains")&&call(spawn,"local/pixelmon/backport/BurstPolicy","spawnRetention"),"Final spawn no longer shares village guard or extra 40-point reduction");
        ClassNode mixin=resource("local/pixelmon/backport/mixin/CapVillageConditionMixin");MethodNode fit=method(mixin,"backport$capVillage");
        require(call(fit,"local/pixelmon/backport/CapSpawns","village")&&call(fit,"com/pixelmonmod/pixelmon/api/util/helpers/WorldHelper","insideStructure")&&constant(fit,1026)&&constant(fit,"village")&&constant(fit,"minecraft:village"),"Species-scoped query bridge/legacy key/other-species fallback missing");
        MethodNode lookup=method(resource("net/minecraft/world/gen/feature/structure/StructureManager"),"func_235010_a_");
        require(call(lookup,"net/minecraft/world/gen/feature/structure/StructureManager","func_235011_a_"),"Structure manager no longer follows start references");
        System.out.println("PASS actual Village->village native registry and start-only cache root causes; native legendary query shares corrected Cap-only full structure references; plains/70/legendary clock/40-point reduction preserved; other species unchanged.");
    }

    static void verifyMagearnaRules(String nativeJar)throws Exception{
        Map<ResourceLocation,JsonElement> data=new LinkedHashMap<>();
        try(JarFile jar=new JarFile(nativeJar)){
            for(var entry:Collections.list(jar.entries())){
                String path=entry.getName(),prefix="data/pixelmon/spawning/";
                if(!path.startsWith(prefix)||!path.endsWith(".json"))continue;
                try(Reader reader=new InputStreamReader(jar.getInputStream(entry),java.nio.charset.StandardCharsets.UTF_8)){
                    data.put(new ResourceLocation("pixelmon",path.substring(prefix.length(),path.length()-5)),new JsonParser().parse(reader));
                }
            }
            MethodNode fits=method(nativeClass(jar,"com/pixelmonmod/pixelmon/api/spawning/conditions/SpawnCondition"),"fits");
            require(call(fits,"com/pixelmonmod/pixelmon/api/util/helpers/WorldHelper","insideStructure"),"Native structure predicate changed");
        }
        ResourceLocation key=new ResourceLocation("pixelmon","legendaries/magearna.set");
        JsonObject original=data.get(key).getAsJsonObject();String originalText=original.toString();
        Map<ResourceLocation,String> before=new HashMap<>();data.forEach((k,v)->before.put(k,v.toString()));
        Content.apply("pixelmon","spawning",data);
        JsonObject expected=new JsonParser().parse(originalText).getAsJsonObject();
        JsonObject condition=expected.getAsJsonArray("spawnInfos").get(0).getAsJsonObject().getAsJsonObject("condition");
        require(condition.getAsJsonArray("stringBiomes").toString().equals("[\"roofed\"]"),"Native biome root cause changed");
        condition.remove("stringBiomes");JsonArray structures=new JsonArray();structures.add("mansion");condition.add("structures",structures);
        JsonObject base=expected.getAsJsonArray("spawnInfos").get(0).getAsJsonObject(),originalColor=base.deepCopy();
        var weight=base.get("rarity").getAsBigDecimal();base.addProperty("rarity",weight.multiply(new java.math.BigDecimal("0.8")).stripTrailingZeros());
        originalColor.addProperty("rarity",weight.multiply(new java.math.BigDecimal("0.2")).stripTrailingZeros());originalColor.addProperty("spec",originalColor.get("spec").getAsString()+" form:original_color");expected.getAsJsonArray("spawnInfos").add(originalColor);
        require(data.get(key).equals(expected),"Only mansion/biome and 80/20 form split may change; timer/total rarity/level/indoors/day/floor must remain native");
        var actual=data.get(key).getAsJsonObject().getAsJsonArray("spawnInfos");
        require(actual.size()==2,"Wrong Magearna form pool size");
        double normalWeight=actual.get(0).getAsJsonObject().get("rarity").getAsDouble(),originalWeight=actual.get(1).getAsJsonObject().get("rarity").getAsDouble();
        require(Math.abs(originalWeight/(normalWeight+originalWeight)-.2)<1e-12,"Original color must be exactly 20% of Magearna weight");
        for(JsonElement info:actual)require(!info.getAsJsonObject().getAsJsonObject("condition").has("stringBiomes")&&info.getAsJsonObject().getAsJsonObject("condition").getAsJsonArray("structures").get(0).getAsString().equals("mansion"),"A Magearna form is still tied to a biome");
        require(original.toString().equals(originalText),"Shared native JSON was mutated");
        for(var entry:data.entrySet())if(!entry.getKey().equals(key))require(entry.getValue().toString().equals(before.get(entry.getKey())),"Unrelated spawn rule changed: "+entry.getKey());
        String once=data.toString();Content.apply("pixelmon","spawning",data);require(once.equals(data.toString()),"Reload duplicated rules");
        var absent=new HashMap<ResourceLocation,JsonElement>();Content.apply("pixelmon","spawning",absent);require(absent.isEmpty(),"Disabled/removed Magearna rule was recreated");
        var foreign=new HashMap<ResourceLocation,JsonElement>();foreign.put(new ResourceLocation("custom","legendaries/magearna.set"),original);Content.apply("pixelmon","spawning",foreign);require(foreign.values().iterator().next()==original,"Foreign data pack entry changed");
        var otherContext=new HashMap<ResourceLocation,JsonElement>();otherContext.put(key,original);Content.apply("custom","spawning",otherContext);require(otherContext.get(key)==original,"Foreign loader context changed");
        ClassNode bridge=resource("local/pixelmon/backport/mixin/CapVillageConditionMixin");MethodNode fit=method(bridge,"backport$capVillage");
        require(constant(fit,801)&&constant(fit,"mansion")&&constant(fit,"minecraft:mansion")&&call(fit,"local/pixelmon/backport/MagearnaSpawns","mansion"),"Species-scoped mansion bridge missing");
        MethodNode mansion=method(resource("local/pixelmon/backport/MagearnaSpawns"),"mansion");
        require(call(mansion,"net/minecraft/world/server/ServerWorld","func_217354_b")&&call(mansion,"net/minecraft/world/gen/feature/structure/StructureManager","func_235010_a_")&&call(mansion,"net/minecraft/world/gen/feature/structure/StructureStart","func_75069_d")&&field(mansion,"net/minecraft/world/gen/feature/structure/Structure","field_236368_d_"),"Loaded native mansion reference/validity lookup missing");
        require(call(method(resource("local/pixelmon/backport/Content"),"apply"),"local/pixelmon/backport/MagearnaSpawns","merge"),"Rule patch not connected to native loader");
        System.out.println("PASS actual native Magearna pool: 80% normal / 20% original color, total encounter weight unchanged; both use mansion structure in every biome; native time/indoors/floor/level/legendary interval retained, unrelated rules untouched, reload idempotent, disabled/foreign pools preserved; cross-chunk mansion reference bridge.");
    }
}
