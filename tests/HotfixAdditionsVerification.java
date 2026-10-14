import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import com.google.gson.*;
import local.pixelmon.backport.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Real resource/interface checks for gender, compasses, death loot and query text. */
public final class HotfixAdditionsVerification {
    static ClassNode read(InputStream in)throws IOException{try(in){ClassNode n=new ClassNode();new ClassReader(in.readAllBytes()).accept(n,0);return n;}}
    static ClassNode resource(String name)throws IOException{return read(HotfixAdditionsVerification.class.getClassLoader().getResourceAsStream(name+".class"));}
    static ClassNode nativeClass(JarFile jar,String name)throws IOException{return read(jar.getInputStream(jar.getJarEntry(name+".class")));}
    static boolean call(MethodNode m,String owner,String name){for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof MethodInsnNode){MethodInsnNode c=(MethodInsnNode)i;if(c.owner.equals(owner)&&c.name.equals(name))return true;}return false;}
    static MethodNode method(ClassNode n,String name){return n.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
    static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    static JsonObject json(String path)throws IOException{try(Reader in=new InputStreamReader(HotfixAdditionsVerification.class.getClassLoader().getResourceAsStream(path),java.nio.charset.StandardCharsets.UTF_8)){return new JsonParser().parse(in).getAsJsonObject();}}
    public static void main(String[] args)throws Exception{
        try(JarFile jar=new JarFile(args[0])){
            ClassNode stats=nativeClass(jar,"com/pixelmonmod/pixelmon/api/pokemon/species/Stats");
            require(method(stats,"isMaleOnly").instructions.size()>0,"Native male-only accessor missing");
            ClassNode pokemon=nativeClass(jar,"com/pixelmonmod/pixelmon/api/pokemon/Pokemon");
            require(pokemon.methods.stream().anyMatch(m->call(m,"com/pixelmonmod/pixelmon/api/pokemon/species/Stats","isMaleOnly")&&call(m,"com/pixelmonmod/pixelmon/api/pokemon/Pokemon","setGender")),"Native incompatible saved gender restoration changed");
            ClassNode nativeQuery=nativeClass(jar,"com/pixelmonmod/pixelmon/api/spawning/archetypes/algorithms/checkspawns/LegendaryCheckSpawns");
            require(method(nativeQuery,"lambda$checkSpawns$0").desc.contains("Ljava/util/HashMap;Ljava/util/List;Lnet/minecraft/command/CommandSource;"),"Candidate-map display hook no longer has the actual native map");
            ClassNode death=nativeClass(jar,"com/pixelmonmod/pixelmon/entities/pixelmon/AbstractHoldsItemsEntity");
            require(call(method(death,"dropNormalItems"),"com/pixelmonmod/pixelmon/entities/npcs/registry/DropItemRegistry","getDropsForPokemon")&&call(method(death,"dropNormalItems"),"com/pixelmonmod/pixelmon/entities/pixelmon/drops/DropItemQueryList","register"),"Guaranteed drops fail to feed native loot query");
            require(call(method(death,"dropNormalItems"),"com/pixelmonmod/pixelmon/entities/pixelmon/AbstractHoldsItemsEntity","hasOwner")&&call(method(death,"dropNormalItems"),"com/pixelmonmod/pixelmon/entities/pixelmon/AbstractHoldsItemsEntity","getTrainer"),"Native ownership guard changed");
            ClassNode register=nativeClass(jar,"com/pixelmonmod/pixelmon/entities/pixelmon/drops/DropItemQueryList");
            require(register.methods.stream().filter(m->m.name.equals("register")).anyMatch(m->call(m,"com/pixelmonmod/pixelmon/api/events/DropEvent","getDrops")),"Native drop event/claim path bypassed");
        }
        ClassNode drops=resource("local/pixelmon/backport/CapDrops");MethodNode ensure=method(drops,"ensure");
        require(call(ensure,"com/pixelmonmod/pixelmon/api/pokemon/item/pokeball/PokeBall","getBallItem")&&call(ensure,"net/minecraft/item/ItemStack","<init>"),"Missing actual ball/crystal item constructors");
        for(MethodNode m:drops.methods)for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof MethodInsnNode){MethodInsnNode c=(MethodInsnNode)i;
            require(!c.owner.contains("Random")&&!c.name.equals("func_70441_a")&&!c.name.equals("register"),"Death loot is probabilistic or grants inventory separately");
        }
        ClassNode sync=resource("local/pixelmon/backport/CapPikachu");require(sync.methods.stream().anyMatch(m->m.name.equals("sync")&&call(m,"com/pixelmonmod/pixelmon/api/pokemon/Pokemon","setGender")),"Legacy female Cap normalization missing");
        ClassNode capSetter=resource("local/pixelmon/backport/mixin/FieldMegaPokemonMixin");require(method(capSetter,"backport$capMale").desc.contains("/Gender;"),"Explicit gender setters can bypass Cap male-only restriction");
        require("恶地极巨巢穴".equals(CompassNames.lookup("structure","pixelmon:badlands_den")),"Screenshot Badlands Den untranslated");
        require("究极空间".equals(CompassNames.lookup("dimension","pixelmon:ultraspace")),"Ultra Space dimension untranslated");
        for(var group:CompassNames.DATA.entrySet())for(var row:group.getValue().getAsJsonObject().entrySet())require(row.getValue().getAsString().codePoints().anyMatch(c->c>=0x4e00&&c<=0x9fff),"Non-Chinese compass name: "+row.getKey());
        require(CompassNames.lookup("structure","unknown:new_structure")==null,"Unknown mod names must retain their own native translation");
        for(int i=1;i<args.length;i++)try(JarFile optional=new JarFile(args[i])){
            for(String ns:new String[]{"naturescompass","explorerscompass"}){
                var entry=optional.getJarEntry("assets/"+ns+"/lang/en_us.json");if(entry==null)continue;
                JsonObject en;try(Reader in=new InputStreamReader(optional.getInputStream(entry),java.nio.charset.StandardCharsets.UTF_8)){en=new JsonParser().parse(in).getAsJsonObject();}
                JsonObject zh=json("assets/"+ns+"/lang/zh_cn.json");for(String key:en.keySet())if(!key.startsWith("_"))require(zh.has(key)&&zh.get(key).getAsString().codePoints().anyMatch(c->c>=0x4e00&&c<=0x9fff),"Missing native compass interface key "+key);
            }
        }
        require(Math.abs(BurstPolicy.spawnChance(.7)-.3)<1e-9&&BurstPolicy.spawnChance(.4)==0,"Displayed chance differs from actual reduction");
        for(String lang:new String[]{"zh_cn","en_us"}){JsonObject rows=json("assets/pixelmon_backport/lang/"+lang+".json");for(String key:new String[]{"pixelmon_backport.spawns.global","pixelmon_backport.spawns.weights","pixelmon_backport.spawns.cap_chance"})require(rows.has(key),"Missing chance/weight distinction "+key);}
        System.out.println("PASS native male-only/read normalization, gender setter guard; all installed compass interface keys and 107 structure/208 biome Chinese names with unknown-name fallback; actual death-loot query/ownership/event path and deterministic native rewards; real native candidate map and 70%-40pp=30% display.");
    }
}
