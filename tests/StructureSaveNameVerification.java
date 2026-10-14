import java.nio.file.*;
import java.net.*;
import java.util.*;
import javax.tools.ToolProvider;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Execute shipped registration and native chunk serialization without a live registry. */
public final class StructureSaveNameVerification {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void add(Map<String,String> sources,String name,String body){int dot=name.lastIndexOf('.');sources.put(name,"package "+name.substring(0,dot)+";"+body);}
    public static void main(String[] args)throws Exception{
        Map<String,String> s=new LinkedHashMap<>();
        add(s,"net.minecraftforge.registries.IForgeRegistryEntry","public interface IForgeRegistryEntry<T> {}");
        add(s,"net.minecraft.world.gen.feature.structure.Structure","public class Structure<T> implements net.minecraftforge.registries.IForgeRegistryEntry<Structure<?>> {public static final com.google.common.collect.BiMap<String,Structure<?>> field_236365_a_=com.google.common.collect.HashBiMap.create();public String func_143025_a(){return field_236365_a_.inverse().get(this);}}");
        add(s,"net.minecraftforge.fml.RegistryObject","public class RegistryObject<T extends net.minecraftforge.registries.IForgeRegistryEntry<?>> {public T value;public net.minecraft.util.ResourceLocation id;public RegistryObject(String n,T v){id=new net.minecraft.util.ResourceLocation(n);value=v;}public T get(){return value;}public net.minecraft.util.ResourceLocation getId(){return id;}}");
        add(s,"net.minecraftforge.registries.DeferredRegister","public class DeferredRegister<T extends IForgeRegistryEntry<?>> {public java.util.Collection<net.minecraftforge.fml.RegistryObject<T>> entries=new java.util.ArrayList<>();public java.util.Collection<net.minecraftforge.fml.RegistryObject<T>> getEntries(){return entries;}}");
        add(s,"fixture.SaveRunner","public class SaveRunner {public static net.minecraftforge.registries.DeferredRegister<net.minecraft.world.gen.feature.structure.Structure<?>> ALIASES;public static void registerNames(){}public static net.minecraft.nbt.CompoundNBT serialize(net.minecraft.util.math.ChunkPos p,java.util.Map<?,?> starts,java.util.Map<?,?> refs){return null;}}");
        add(s,"fixture.SaveCases","public class SaveCases {static void check(boolean b){if(!b)throw new AssertionError();}public static void run()throws Exception {SaveRunner.ALIASES=new net.minecraftforge.registries.DeferredRegister<>();java.util.Map<net.minecraft.world.gen.feature.structure.Structure<?>,it.unimi.dsi.fastutil.longs.LongSet> refs=new java.util.LinkedHashMap<>();for(int i=0;i<38;i++){var structure=new net.minecraft.world.gen.feature.structure.Structure<>();SaveRunner.ALIASES.entries.add(new net.minecraftforge.fml.RegistryObject<>(\"pixelmon_backport:alias_\"+i,structure));refs.put(structure,new it.unimi.dsi.fastutil.longs.LongOpenHashSet(new long[]{(-173L<<32)|(-52&0xffffffffL)}));}boolean rejected=false;try{SaveRunner.serialize(new net.minecraft.util.math.ChunkPos(0,0),java.util.Map.of(),refs).func_74734_a(new java.io.DataOutputStream(new java.io.ByteArrayOutputStream()));}catch(NullPointerException e){rejected=true;}check(rejected);SaveRunner.registerNames();SaveRunner.registerNames();var nbt=SaveRunner.serialize(new net.minecraft.util.math.ChunkPos(0,0),java.util.Map.of(),refs);var bytes=new java.io.ByteArrayOutputStream();net.minecraft.nbt.CompressedStreamTools.func_74800_a(nbt,new java.io.DataOutputStream(bytes));var loaded=net.minecraft.nbt.CompressedStreamTools.func_74794_a(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())));var savedRefs=loaded.func_74775_l(\"References\");check(savedRefs.func_150296_c().size()==38);for(var alias:SaveRunner.ALIASES.entries){String id=alias.getId().toString();check(id.equals(alias.get().func_143025_a()));check(savedRefs.func_197645_o(id)[0]==((-173L<<32)|(-52&0xffffffffL)));}var entry=SaveRunner.ALIASES.entries.iterator().next();net.minecraft.world.gen.feature.structure.Structure.field_236365_a_.put(entry.getId().toString(),new net.minecraft.world.gen.feature.structure.Structure<>());boolean conflict=false;try{SaveRunner.registerNames();}catch(IllegalStateException e){conflict=true;}check(conflict);}}");
        Path dir=Files.createTempDirectory(Paths.get(".cache"),"save-name-fixture-");
        List<String> options=new ArrayList<>(List.of("--release","11","-proc:none","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
        for(var row:s.entrySet()){Path path=dir.resolve(row.getKey().replace('.','/')+".java");Files.createDirectories(path.getParent());Files.writeString(path,row.getValue());options.add(path.toString());}
        check(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))==0,"Save fixtures did not compile");
        ClassNode runner=new ClassNode();new ClassReader(Files.readAllBytes(dir.resolve("fixture/SaveRunner.class"))).accept(runner,0);
        runner.methods.removeIf(m->m.name.equals("registerNames")||m.name.equals("serialize"));
        MethodNode registration=Version13StaticVerification.node("local/pixelmon/backport/GymLocator").methods.stream().filter(m->m.name.equals("registerNames")).findFirst().orElseThrow();
        for(AbstractInsnNode i:registration.instructions)if(i instanceof FieldInsnNode){var f=(FieldInsnNode)i;if(f.owner.equals("local/pixelmon/backport/GymLocator")&&f.name.equals("ALIASES"))f.owner="fixture/SaveRunner";}
        runner.methods.add(registration);
        MethodNode nativeWriter=Version13StaticVerification.node("net/minecraft/world/chunk/storage/ChunkSerializer").methods.stream().filter(m->m.name.equals("func_222649_a")).findFirst().orElseThrow();
        nativeWriter.name="serialize";nativeWriter.access=Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC;runner.methods.add(nativeWriter);
        ClassWriter writer=new ClassWriter(0);runner.accept(writer);Files.write(dir.resolve("fixture/SaveRunner.class"),writer.toByteArray());
        try(URLClassLoader loader=new URLClassLoader(new URL[]{dir.toUri().toURL()},StructureSaveNameVerification.class.getClassLoader()){
            protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
                synchronized(getClassLoadingLock(name)){Class<?> c=findLoadedClass(name);if(c==null&&s.containsKey(name))c=findClass(name);if(c==null)c=super.loadClass(name,false);if(resolve)resolveClass(c);return c;}
            }
        }){loader.loadClass("fixture.SaveCases").getMethod("run").invoke(null);}
        System.out.println("PASS native chunk reference serializer reproduces null-name writeUTF failure; shipped registration fixes all 38 aliases, preserves negative references across NBT roundtrip, is idempotent and rejects ID conflicts.");
    }
}
