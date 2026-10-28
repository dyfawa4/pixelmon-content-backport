package local.pixelmon.backport;
import java.util.Set;
import net.minecraft.util.ResourceLocation;
public final class UltraDenDensity {
    private UltraDenDensity(){}
    public static final Set<String> IDS=Set.of("pixelmon:ultra_desert_den","pixelmon:ultra_deep_sea_den",
        "pixelmon:ultra_forest_den","pixelmon:ultra_jungle_den","pixelmon:ultra_plant_den","pixelmon:ultra_crater_den");
    public static boolean keep(ResourceLocation id,long seed,int x,int z){
        if(id==null||!IDS.contains(id.toString()))return true;
        long value=seed^0x4d5c1a9bd7e2f603L^((long)x*0x9e3779b97f4a7c15L)^((long)z*0xc2b2ae3d27d4eb4fL)^((long)id.toString().hashCode()*0x165667b19e3779f9L);
        value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;value=(value^(value>>>27))*0x94d049bb133111ebL;value^=value>>>31;
        return (value>>>11)*0x1.0p-53>=.3;
    }
}
