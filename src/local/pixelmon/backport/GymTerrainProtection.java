package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.world.structure.PixelmonStructures;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.util.math.SectionPos;

/** Planned gym references exist before biome decoration, across chunk boundaries. */
public final class GymTerrainProtection {
    private GymTerrainProtection(){}
    public static boolean intersects(int minX,int minZ,int maxX,int maxZ,int cityMinX,int cityMinZ,int cityMaxX,int cityMaxZ){
        return maxX>=cityMinX && minX<=cityMaxX && maxZ>=cityMinZ && minZ<=cityMaxZ;
    }
    public static boolean protectedArea(ISeedReader world,int minX,int minZ,int maxX,int maxZ){
        // Player-grown saplings/commands use ServerWorld, not biome decoration.
        if(!(world instanceof WorldGenRegion))return false;
        for(int x=Math.floorDiv(minX,16);x<=Math.floorDiv(maxX,16);x++)for(int z=Math.floorDiv(minZ,16);z<=Math.floorDiv(maxZ,16);z++){
            // Read existing generation references; never load or decorate more chunks.
            if(!world.func_217354_b(x,z))continue;
            try(var starts=world.func_241827_a(SectionPos.func_218154_a(x,0,z),PixelmonStructures.GYMS.get())){
                if(starts.anyMatch(start->{if(!start.func_75069_d())return false;var box=start.func_75071_a();
                    return intersects(minX,minZ,maxX,maxZ,box.field_78897_a,box.field_78896_c,box.field_78893_d,box.field_78892_f);}))return true;
            }
        }
        return false;
    }
}
