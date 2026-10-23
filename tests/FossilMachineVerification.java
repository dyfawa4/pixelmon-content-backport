import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import javax.tools.ToolProvider;
import com.google.gson.JsonParser;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

/** Execute the real native tick and NBT/packet methods with controlled world transport. */
public final class FossilMachineVerification {
    static final String P="com.pixelmonmod.pixelmon.", TILE=P+"blocks.tileentity.FossilMachineTileEntity";
    static void add(Map<String,String> sources,String name,String text){sources.put(name,"package "+name.substring(0,name.lastIndexOf('.'))+";"+text);}
    public static void main(String[] args)throws Exception{
        MixinBootstrap.init();MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);Mixins.addConfiguration("pixelmon_backport.mixins.json");
        var phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.DEFAULT);
        var ctor=Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer").getDeclaredConstructor();ctor.setAccessible(true);IMixinTransformer transformer=(IMixinTransformer)ctor.newInstance();
        byte[] original;try(var jar=new JarFile(args[0])){original=jar.getInputStream(jar.getJarEntry(TILE.replace('.','/')+".class")).readAllBytes();}
        byte[] fixed=transformer.transformClassBytes(TILE,TILE,original);
        Map<String,String> s=new LinkedHashMap<>();
        add(s,"net.minecraft.util.math.BlockPos","public class BlockPos {int x;public BlockPos(int x){this.x=x;}public int func_177958_n(){return x;}public int func_177956_o(){return 64;}public int func_177952_p(){return 0;}public int hashCode(){return x;}public boolean equals(Object p){return p instanceof BlockPos&&((BlockPos)p).x==x;}}");
        add(s,"net.minecraft.block.BlockState","public class BlockState {}");
        add(s,"net.minecraft.nbt.CompoundNBT","public class CompoundNBT {java.util.Map<String,Object> data=new java.util.HashMap<>();public void func_74778_a(String k,String v){data.put(k,v);}public void func_74768_a(String k,int v){data.put(k,v);}public void func_74776_a(String k,float v){data.put(k,v);}public void func_74757_a(String k,boolean v){data.put(k,v);}public boolean func_74764_b(String k){return data.containsKey(k);}public boolean func_150297_b(String k,int t){return data.containsKey(k);}public String func_74779_i(String k){return (String)data.getOrDefault(k,\"\");}public int func_74762_e(String k){return ((Number)data.getOrDefault(k,0)).intValue();}public float func_74760_g(String k){return ((Number)data.getOrDefault(k,0)).floatValue();}public boolean func_74767_n(String k){return Boolean.TRUE.equals(data.get(k));}}");
        add(s,"net.minecraft.network.NetworkManager","public class NetworkManager {}");
        add(s,"net.minecraft.network.play.server.SUpdateTileEntityPacket","public class SUpdateTileEntityPacket {public net.minecraft.util.math.BlockPos pos;net.minecraft.nbt.CompoundNBT data;public SUpdateTileEntityPacket(net.minecraft.util.math.BlockPos p,int type,net.minecraft.nbt.CompoundNBT n){pos=p;data=n;}public net.minecraft.nbt.CompoundNBT func_148857_g(){return data;}}");
        add(s,"net.minecraft.world.World","public class World {public boolean field_72995_K;public java.util.Random field_73012_v=new java.util.Random(1);public int renderNotifications;public void func_225319_b(net.minecraft.util.math.BlockPos p,net.minecraft.block.BlockState a,net.minecraft.block.BlockState b){renderNotifications++;}}");
        add(s,"net.minecraft.world.server.ServerChunkProvider","public class ServerChunkProvider {public java.util.Map<net.minecraft.util.math.BlockPos,"+TILE+"> server=new java.util.HashMap<>(),client=new java.util.HashMap<>();java.util.Set<net.minecraft.util.math.BlockPos> pending=new java.util.HashSet<>();public int packets;public void func_217217_a(net.minecraft.util.math.BlockPos p){pending.add(p);}public void flush(){for(var p:pending){client.get(p).onDataPacket(null,server.get(p).func_189518_D_());packets++;}pending.clear();}}");
        add(s,"net.minecraft.world.server.ServerWorld","public class ServerWorld extends net.minecraft.world.World {public ServerChunkProvider chunks=new ServerChunkProvider();public ServerChunkProvider func_72863_F(){return chunks;}}");
        add(s,"net.minecraft.tileentity.TileEntity","public class TileEntity {public net.minecraft.world.World field_145850_b;public net.minecraft.util.math.BlockPos field_174879_c=new net.minecraft.util.math.BlockPos(0);public int dirty;public void func_70296_d(){dirty++;}public net.minecraft.world.World func_145831_w(){return field_145850_b;}public net.minecraft.util.math.BlockPos func_174877_v(){return field_174879_c;}public net.minecraft.block.BlockState func_195044_w(){return new net.minecraft.block.BlockState();}public net.minecraft.nbt.CompoundNBT func_189515_b(net.minecraft.nbt.CompoundNBT n){return n;}public void func_230337_a_(net.minecraft.block.BlockState b,net.minecraft.nbt.CompoundNBT n){}}");
        add(s,P+"api.pokemon.species.Species","public class Species {}");
        add(s,P+"enums.items.EnumFossils","public enum EnumFossils {NULL,HELIX;public "+P+"api.pokemon.species.Species getPokemon(){return this==NULL?null:new "+P+"api.pokemon.species.Species();}public int getIndex(){return ordinal();}public static EnumFossils fromIndex(int i){return values()[i];}}");
        add(s,P+"api.util.helpers.WorldHelper","public class WorldHelper {public static void spawnRedstoneParticle(net.minecraft.world.World w,double x,double y,double z,double a,double b,double c){}}");
        add(s,"net.minecraft.item.Item","public class Item {public static Item func_150899_d(int id){return null;}}");
        add(s,P+"items.FossilItem","public class FossilItem extends net.minecraft.item.Item {public "+P+"enums.items.EnumFossils fossil;}");
        add(s,P+"api.pokemon.item.pokeball.PokeBall","public class PokeBall {String name;public PokeBall(String n){name=n;}public String getName(){return name;}}");
        add(s,"com.pixelmonmod.api.registry.RegistryValue","public class RegistryValue<T> {T value;public RegistryValue(T v){value=v;}public T orElse(T fallback){return value==null?fallback:value;}}");
        add(s,P+"api.pokemon.item.pokeball.PokeBallRegistry","public class PokeBallRegistry {public static com.pixelmonmod.api.registry.RegistryValue<PokeBall> getPokeBall(String n){return new com.pixelmonmod.api.registry.RegistryValue<>(new PokeBall(n));}}");
        add(s,TILE,"public class FossilMachineTileEntity extends net.minecraft.tileentity.TileEntity {public float fossilProgress,pokemonProgress,fossilJitter;public int completionRate,completionSync,dotTicks,screenFlickerTick;public String dots=\"\",currentPokemon=\"\";public boolean pokemonOccupied,isShiny,staticFlicker;public "+P+"enums.items.EnumFossils currentFossil="+P+"enums.items.EnumFossils.NULL;public "+P+"api.pokemon.item.pokeball.PokeBall pokeball;public void swapFossilForPokemon(){currentPokemon=\"Omanyte\";pokemonOccupied=true;currentFossil="+P+"enums.items.EnumFossils.NULL;}public void func_73660_a(){}public net.minecraft.nbt.CompoundNBT func_189517_E_(){return null;}public net.minecraft.network.play.server.SUpdateTileEntityPacket func_189518_D_(){return null;}public void onDataPacket(net.minecraft.network.NetworkManager n,net.minecraft.network.play.server.SUpdateTileEntityPacket p){}}");
        s.put("fixture.FossilCases",Files.readString(Path.of("tests/resources/FossilCases.java.txt")));
        Path dir=Files.createTempDirectory(Path.of(".cache/fossil-151"),"fixture-");List<String> options=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-d",dir.toString()));
        for(var row:s.entrySet()){Path path=dir.resolve(row.getKey().replace('.','/')+".java");Files.createDirectories(path.getParent());Files.writeString(path,row.getValue());options.add(path.toString());}
        if(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))!=0)throw new AssertionError("Fixture compilation failed");
        install(dir,original);run(dir,"nativeStall");install(dir,fixed);run(dir,"fixedCases");
        var locale=new JsonParser().parse(Files.readString(Path.of("resources/assets/pixelmon_backport/lang/zh_cn.json"))).getAsJsonObject();
        if(!locale.get("pixelmon.backport.cap.forbidden_ball").getAsString().equals("大师球、起源球和公园球无法捕捉戴帽子的皮卡丘，请换一种精灵球。"))throw new AssertionError("Cap wording");
        System.out.println("PASS fossil native 33% stall reproduced; patched tick and native NBT/update packets progress to 100%, completion timing/ball/shiny/independent machines/reload/idle preserved; cap wording updated.");
    }
    static void install(Path dir,byte[] bytes)throws Exception{
        Path path=dir.resolve(TILE.replace('.','/')+".class");ClassNode target=new ClassNode(),source=new ClassNode();new ClassReader(Files.readAllBytes(path)).accept(target,0);new ClassReader(bytes).accept(source,0);
        Set<String> names=Set.of("func_73660_a","func_189515_b","func_230337_a_","func_189517_E_","func_189518_D_","onDataPacket");
        for(MethodNode m:source.methods)if(names.contains(m.name)||m.name.contains("backport$syncProgress")){target.methods.removeIf(old->old.name.equals(m.name)&&old.desc.equals(m.desc));target.methods.add(m);}
        ClassWriter writer=new ClassWriter(0);target.accept(writer);Files.write(path,writer.toByteArray());
    }
    static void run(Path dir,String method)throws Exception{
        try(var loader=new URLClassLoader(new URL[]{dir.toUri().toURL()},FossilMachineVerification.class.getClassLoader()){
            protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{synchronized(getClassLoadingLock(name)){
                if(!name.startsWith("net.minecraft.")&&!name.startsWith("com.pixelmonmod.")&&!name.startsWith("fixture.")&&!name.equals("local.pixelmon.backport.FossilMachineSync"))return super.loadClass(name,resolve);
                Class<?> c=findLoadedClass(name);if(c==null){try{c=findClass(name);}catch(ClassNotFoundException missing){try(var in=getParent().getResourceAsStream(name.replace('.','/')+".class")){byte[] bytes=in.readAllBytes();c=defineClass(name,bytes,0,bytes.length);}catch(Exception fail){throw new ClassNotFoundException(name,fail);}}}if(resolve)resolveClass(c);return c;
            }}
        }){try{loader.loadClass("fixture.FossilCases").getMethod(method).invoke(null);}catch(java.lang.reflect.InvocationTargetException fail){throw new AssertionError(method,fail.getCause());}}
    }
}
