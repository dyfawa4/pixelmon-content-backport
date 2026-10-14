import java.nio.file.*;
import java.net.*;
import java.util.*;
import javax.tools.ToolProvider;

/** Replay the reported outside-region start through the shipped protection helper. */
public final class GymRegionBoundaryVerification {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void add(Map<String,String> sources,String name,String body){int dot=name.lastIndexOf('.');sources.put(name,"package "+name.substring(0,dot)+";"+body);}
    public static void main(String[] args)throws Exception{
        Map<String,String> s=new LinkedHashMap<>();
        add(s,"net.minecraft.world.ISeedReader","public interface ISeedReader {}");
        add(s,"net.minecraft.world.chunk.ChunkStatus","public class ChunkStatus {public static final ChunkStatus field_222606_b=new ChunkStatus(),field_222607_c=new ChunkStatus();}");
        add(s,"net.minecraftforge.registries.IForgeRegistryEntry","public interface IForgeRegistryEntry<T> {}");
        add(s,"net.minecraft.world.gen.feature.structure.Structure","public class Structure<T> implements net.minecraftforge.registries.IForgeRegistryEntry<Structure<?>> {}");
        add(s,"net.minecraft.util.math.ChunkPos","public class ChunkPos {public static int func_212578_a(long p){return (int)p;}public static int func_212579_b(long p){return (int)(p>>>32);}}");
        add(s,"net.minecraft.util.math.MutableBoundingBox","public class MutableBoundingBox {public int field_78897_a,field_78896_c,field_78893_d,field_78892_f;public MutableBoundingBox(int x,int z,int X,int Z){field_78897_a=x;field_78896_c=z;field_78893_d=X;field_78892_f=Z;}}");
        add(s,"net.minecraft.world.gen.feature.structure.StructureStart","public class StructureStart<T> {public boolean valid=true;public net.minecraft.util.math.MutableBoundingBox box;public boolean func_75069_d(){return valid;}public net.minecraft.util.math.MutableBoundingBox func_75071_a(){return box;}}");
        add(s,"net.minecraftforge.fml.RegistryObject","public class RegistryObject<T extends net.minecraftforge.registries.IForgeRegistryEntry<?>> {public T value;public T get(){return value;}}");
        add(s,"com.pixelmonmod.pixelmon.world.structure.type.GymStructure","public class GymStructure extends net.minecraft.world.gen.feature.structure.Structure<Object> {}");
        add(s,"com.pixelmonmod.pixelmon.world.structure.PixelmonStructures","public class PixelmonStructures {public static final net.minecraftforge.fml.RegistryObject<com.pixelmonmod.pixelmon.world.structure.type.GymStructure> GYMS=new net.minecraftforge.fml.RegistryObject<>();static{GYMS.value=new com.pixelmonmod.pixelmon.world.structure.type.GymStructure();}}");
        add(s,"net.minecraft.world.IStructureReader","public interface IStructureReader {it.unimi.dsi.fastutil.longs.LongSet func_230346_b_(net.minecraft.world.gen.feature.structure.Structure<?> s);net.minecraft.world.gen.feature.structure.StructureStart<?> func_230342_a_(net.minecraft.world.gen.feature.structure.Structure<?> s);}");
        add(s,"net.minecraft.world.chunk.IChunk","public interface IChunk extends net.minecraft.world.IStructureReader {}");
        add(s,"net.minecraft.world.gen.WorldGenRegion","public class WorldGenRegion implements net.minecraft.world.ISeedReader {public java.util.Map<Long,net.minecraft.world.chunk.IChunk> chunks=new java.util.HashMap<>();public int reads;public net.minecraft.world.chunk.IChunk func_217353_a(int x,int z,net.minecraft.world.chunk.ChunkStatus status,boolean required){reads++;if(required)throw new AssertionError(\"Unbounded region read\");return chunks.get(((long)z<<32)|(x&0xffffffffL));}}");
        add(s,"fixture.RegionCases","public class RegionCases {static void check(boolean b){if(!b)throw new AssertionError();}static class Chunk implements net.minecraft.world.chunk.IChunk {it.unimi.dsi.fastutil.longs.LongSet refs=new it.unimi.dsi.fastutil.longs.LongOpenHashSet();net.minecraft.world.gen.feature.structure.StructureStart<?> start;public it.unimi.dsi.fastutil.longs.LongSet func_230346_b_(net.minecraft.world.gen.feature.structure.Structure<?> s){return refs;}public net.minecraft.world.gen.feature.structure.StructureStart<?> func_230342_a_(net.minecraft.world.gen.feature.structure.Structure<?> s){return start;}}public static void run(){var r=new net.minecraft.world.gen.WorldGenRegion();check(!local.pixelmon.backport.GymTerrainProtection.protectedArea(r,-16,32,-1,47));Chunk holder=new Chunk();r.chunks.put((2L<<32)|0xffffffffL,holder);check(!local.pixelmon.backport.GymTerrainProtection.protectedArea(r,-16,32,-1,47));long outside=(-173L<<32)|(-52&0xffffffffL);holder.refs.add(outside);check(local.pixelmon.backport.GymTerrainProtection.protectedArea(r,-16,32,-1,47));Chunk source=new Chunk();r.chunks.put(outside,source);check(!local.pixelmon.backport.GymTerrainProtection.protectedArea(r,-16,32,-1,47));var start=new net.minecraft.world.gen.feature.structure.StructureStart<>();source.start=start;start.box=new net.minecraft.util.math.MutableBoundingBox(16,32,31,47);check(!local.pixelmon.backport.GymTerrainProtection.protectedArea(r,-16,32,-1,47));start.box=new net.minecraft.util.math.MutableBoundingBox(-1,47,31,80);check(local.pixelmon.backport.GymTerrainProtection.protectedArea(r,-16,32,-1,47));start.valid=false;check(!local.pixelmon.backport.GymTerrainProtection.protectedArea(r,-16,32,-1,47));check(!local.pixelmon.backport.GymTerrainProtection.protectedArea(new net.minecraft.world.ISeedReader(){},-16,32,-1,47));check(r.reads<20);}}");
        Path dir=Files.createTempDirectory(Paths.get(".cache"),"region-fixture-");
        List<String> options=new ArrayList<>(List.of("--release","11","-proc:none","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
        for(var row:s.entrySet()){Path path=dir.resolve(row.getKey().replace('.','/')+".java");Files.createDirectories(path.getParent());Files.writeString(path,row.getValue());options.add(path.toString());}
        check(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))==0,"Boundary fixtures did not compile");
        try(URLClassLoader loader=new URLClassLoader(new URL[]{dir.toUri().toURL()},GymRegionBoundaryVerification.class.getClassLoader()){
            protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
                synchronized(getClassLoadingLock(name)){Class<?> c=findLoadedClass(name);
                    if(c==null&&(s.containsKey(name)||name.startsWith("fixture.RegionCases$")))c=findClass(name);
                    if(c==null&&name.equals("local.pixelmon.backport.GymTerrainProtection"))try(var in=getParent().getResourceAsStream(name.replace('.','/')+".class")){byte[] b=in.readAllBytes();c=defineClass(name,b,0,b.length);}catch(Exception e){throw new ClassNotFoundException(name,e);}
                    if(c==null)c=super.loadClass(name,false);if(resolve)resolveClass(c);return c;}
            }
        }){loader.loadClass("fixture.RegionCases").getMethod("run").invoke(null);}
        System.out.println("PASS shipped terrain helper: reported [-52,-173] outside start, absent holder/start, empty refs, exact overlap/nonoverlap, invalid starts, negative coordinates and player-world exclusion; no required region reads.");
    }
}
