package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.world.structure.PixelmonStructures;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;

/** Planned gym references exist before biome decoration, across chunk boundaries. */
public final class GymTerrainProtection {
    private GymTerrainProtection(){}
    public static boolean intersects(int minX,int minZ,int maxX,int maxZ,int cityMinX,int cityMinZ,int cityMaxX,int cityMaxZ){
        return maxX>=cityMinX && minX<=cityMaxX && maxZ>=cityMinZ && minZ<=cityMaxZ;
    }
    public static boolean protectedArea(ISeedReader world,int minX,int minZ,int maxX,int maxZ){
        // Player-grown saplings/commands use ServerWorld, not biome decoration.
        if(!(world instanceof WorldGenRegion))return false;
        WorldGenRegion region=(WorldGenRegion)world;
        for(int x=Math.floorDiv(minX,16);x<=Math.floorDiv(maxX,16);x++)for(int z=Math.floorDiv(minZ,16);z<=Math.floorDiv(maxZ,16);z++){
            // A reference holder can be in this region while its start is outside it.
            // StructureManager's lazy stream requires every referenced chunk and throws
            // at the region boundary. Both reads here explicitly permit a missing chunk.
            var holder=region.func_217353_a(x,z,ChunkStatus.field_222607_c,false);
            if(holder==null)continue;
            var refs=holder.func_230346_b_(PixelmonStructures.GYMS.get()).iterator();
            while(refs.hasNext()){
                long ref=refs.nextLong();
                var source=region.func_217353_a(ChunkPos.func_212578_a(ref),ChunkPos.func_212579_b(ref),ChunkStatus.field_222606_b,false);
                // An actual gym reference identifies a city boundary chunk. Protect it
                // conservatively if the exact footprint is unavailable in this region.
                if(source==null)return true;
                var start=source.func_230342_a_(PixelmonStructures.GYMS.get());
                if(start==null || !start.func_75069_d())continue;
                var box=start.func_75071_a();
                if(intersects(minX,minZ,maxX,maxZ,box.field_78897_a,box.field_78896_c,box.field_78893_d,box.field_78892_f))return true;
            }
        }
        return false;
    }
}
