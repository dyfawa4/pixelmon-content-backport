package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.world.structure.type.BiomeGenericStructure;
import com.pixelmonmod.pixelmon.world.dimension.ultraspace.UltraSpaceBiomeProvider;
import com.pixelmonmod.pixelmon.world.dimension.ultraspace.generator.UltraDeepSeaChunkGenerator;
import local.pixelmon.backport.UltraDenDensity;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** The six actual Ultra raid dens use BiomeGenericStructure, not DenStructure. */
@Mixin(value=BiomeGenericStructure.class,remap=false)
public abstract class UltraDenDensityMixin {
    @Inject(method="isFeatureChunk(Lnet/minecraft/world/gen/ChunkGenerator;Lnet/minecraft/world/biome/provider/BiomeProvider;JLnet/minecraft/util/SharedSeedRandom;IILnet/minecraft/world/biome/Biome;Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/gen/feature/NoFeatureConfig;)Z",at=@At("RETURN"),cancellable=true)
    private void backport$denDensity(ChunkGenerator generator,BiomeProvider provider,long seed,SharedSeedRandom random,
        int x,int z,Biome biome,ChunkPos chunk,NoFeatureConfig feature,CallbackInfoReturnable<Boolean> cir){
        if(cir.getReturnValue()&&(generator instanceof UltraDeepSeaChunkGenerator||provider instanceof UltraSpaceBiomeProvider)
            &&!UltraDenDensity.keep(((BiomeGenericStructure)(Object)this).getRegistryName(),seed,x,z))cir.setReturnValue(false);
    }
}
