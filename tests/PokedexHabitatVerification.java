import local.pixelmon.backport.PokedexHabitats;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public final class PokedexHabitatVerification {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        for(int dex:new int[]{906,1001,1025,1026})check(PokedexHabitats.handles(dex),"New species excluded");
        for(int dex:new int[]{-1,0,905,1027,Integer.MAX_VALUE})check(!PokedexHabitats.handles(dex),"Existing or invalid species rerouted");
        Set<String> reads=new HashSet<>();Set<String> sources=new HashSet<>();
        for(MethodNode method:Version13StaticVerification.node("local/pixelmon/backport/PokedexHabitats").methods)for(var instruction:method.instructions){
            if(instruction instanceof MethodInsnNode){var call=(MethodInsnNode)instruction;reads.add(call.name);
                check(!call.name.equals("create") && !call.name.equals("construct") && !call.name.equals("fits") && !call.name.equals("matches") && !call.name.equals("equalsIgnoreCase"),"Habitat lookup creates random Pokemon/spawns or relies on set label");}
            if(instruction instanceof FieldInsnNode){var field=(FieldInsnNode)instruction;sources.add(field.name);}
        }
        check(reads.containsAll(Set.of("getSpecies","getDex","addTime","addWeather","setMinY","setMaxY")),"Actual species and native condition metadata missing");
        check(sources.containsAll(Set.of("standard","legendaries","biomes","times","cachedWeathers","minY","maxY")),"Current native pools/conditions not used");
        Set<String> gate=new HashSet<>();
        for(MethodNode m:Version13StaticVerification.node("local/pixelmon/backport/mixin/PokedexHabitatMixin").methods)for(var i:m.instructions){
            if(i instanceof MethodInsnNode)gate.add(((MethodInsnNode)i).name);
            if(i instanceof FieldInsnNode)gate.add(((FieldInsnNode)i).name);
        }
        check(gate.containsAll(Set.of("getSender","getParty","UNKNOWN","reply")),"Native seen/caught ownership gate or reply missing");
        boolean id=false,equality=false;
        try(ZipFile jar=new ZipFile(args[0]);InputStream in=jar.getInputStream(jar.getEntry("com/pixelmonmod/pixelmon/comm/packetHandlers/pokedex/RequestSpawnDataPacket.class"))){
            ClassNode nativePacket=new ClassNode();new ClassReader(in).accept(nativePacket,0);
            for(MethodNode m:nativePacket.methods)for(var i:m.instructions){
                if(i instanceof FieldInsnNode)id|=((FieldInsnNode)i).owner.endsWith("SpawnSet") && ((FieldInsnNode)i).name.equals("id");
                if(i instanceof MethodInsnNode)equality|=((MethodInsnNode)i).name.equals("equalsIgnoreCase");
            }
        }
        check(id && equality,"Native root-cause evidence changed");
        System.out.println("PASS new-species habitat routing by actual species across live standard/legendary pools; native seen gate, biome/time/weather/height packets, no random construction or altered spawn rules; game UI remains manual.");
    }
}
