import java.util.*;
import net.minecraft.util.math.*;
import local.pixelmon.backport.StrongholdLocator;
import org.objectweb.asm.tree.*;

public final class StrongholdVerification {
    static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);}
    public static void main(String[] args)throws Exception{
        var rings=new ArrayList<>(List.of(new ChunkPos(-200,0),new ChunkPos(100,0),new ChunkPos(0,-100)));
        var origin=new BlockPos(0,130,0);var sorted=StrongholdLocator.ordered(rings,origin);
        check(sorted.get(0).equals(new BlockPos(8,32,-1592)),"Wrong nearest ring at negative coordinates");
        check(sorted.get(1).equals(new BlockPos(1608,32,8)),"Ring order differs from distance");
        check(rings.get(0).field_77276_a==-200,"Native ring cache mutated");
        check(StrongholdLocator.distance(sorted.get(0),origin)==StrongholdLocator.distance(sorted.get(0),new BlockPos(0,0,0)),"Compass radius must remain horizontal");
        var distant=new BlockPos(12548484,78,-1584848);var nearby=StrongholdLocator.ordered(List.of(new ChunkPos(784280,-99053)),distant).get(0);
        check(nearby.equals(new BlockPos(12548488,32,-1584840))&&StrongholdLocator.distance(nearby,distant)==80,"Far ring coordinates lost precision");
        check(StrongholdLocator.ordered(Collections.emptyList(),origin).isEmpty(),"Empty rings invented a result");
        ClassNode generator=Version13StaticVerification.node("net/minecraft/world/gen/ChunkGenerator");
        MethodNode locate=generator.methods.stream().filter(m->m.name.equals("func_235956_a_")).findFirst().orElseThrow();
        boolean special=false,init=false,cache=false;
        for(var i:locate.instructions.toArray()){if(i instanceof FieldInsnNode){var f=(FieldInsnNode)i;special|=f.name.equals("field_236375_k_");cache|=f.name.equals("field_235951_f_");}if(i instanceof MethodInsnNode)init|=((MethodInsnNode)i).name.equals("func_235958_g_");}
        check(special&&init&&cache,"Actual vanilla /locate stronghold ring route changed");
        ClassNode worker=Version13StaticVerification.node("local/pixelmon/backport/mixin/CompassStrongholdSearchMixin");
        Set<String> calls=new HashSet<>();for(MethodNode m:worker.methods)for(var i:m.instructions.toArray())if(i instanceof MethodInsnNode)calls.add(((MethodInsnNode)i).name);
        check(calls.containsAll(Set.of("available","candidates","alreadyFound","hasWork","finish")),"Compass availability, cache, limits or final status omitted");
        check(!calls.contains("func_236392_a_")&&!calls.contains("func_212849_a_"),"Stronghold route still uses random spacing or materializes chunks");
        System.out.println("PASS original ring-center coordinates and sorted copy at negative/12.5M positions, horizontal limits/empty cache; native /locate ring evidence and dedicated compass status/cache/cancel wiring, no random-spacing search or chunk materialization.");
    }
}
