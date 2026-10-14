package local.pixelmon.backport;

import java.util.*;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import local.pixelmon.backport.mixin.StrongholdGeneratorAccess;

public final class StrongholdLocator {
    private StrongholdLocator(){}
    public static boolean handles(Structure<?> structure){return structure==Structure.field_236375_k_;}
    public static boolean available(ServerWorld world){
        var generator=world.func_72863_F().func_201711_g();
        var rings=generator.func_235957_b_().func_236199_b_();
        return rings!=null && rings.func_236663_c_()>0
            && world.func_73046_m().func_240793_aU_().func_230418_z_().func_236222_c_()
            && generator.func_202090_b().func_205004_a(Structure.field_236375_k_);
    }
    public static List<BlockPos> candidates(ServerWorld world,BlockPos origin){
        var generator=(StrongholdGeneratorAccess)world.func_72863_F().func_201711_g();
        generator.backport$initializeStrongholds();
        return ordered(generator.backport$strongholdRings(),origin);
    }
    /** Native /locate uses the center of each ring chunk at Y32. */
    public static List<BlockPos> ordered(List<ChunkPos> rings,BlockPos origin){
        List<BlockPos> points=new ArrayList<>();for(ChunkPos p:rings)points.add(new BlockPos((p.field_77276_a<<4)+8,32,(p.field_77275_b<<4)+8));
        points.sort(Comparator.comparingLong(p->distance(p,origin)));return points;
    }
    public static long distance(BlockPos a,BlockPos b){long x=(long)a.func_177958_n()-b.func_177958_n(),z=(long)a.func_177952_p()-b.func_177952_p();return x*x+z*z;}
    public static boolean alreadyFound(PlayerEntity player,BlockPos point){
        try{
            Object cache=Class.forName("com.chaosthedude.explorerscompass.util.StructureUtils").getField("haveFound").get(null);
            for(Object value:(List<?>)cache){Pair<?,?> row=(Pair<?,?>)value;if(!player.func_110124_au().equals(row.getFirst()))continue;
                Pair<?,?> pos=(Pair<?,?>)row.getSecond();if(Integer.valueOf(point.func_177958_n()).equals(pos.getFirst())&&Integer.valueOf(point.func_177952_p()).equals(pos.getSecond()))return true;
            }
            return false;
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Stronghold compass cache unavailable",e);}
    }
}
