package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.world.structure.type.HauntedTowerStructure;
import net.minecraft.world.gen.*;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.biome.*;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=HauntedTowerStructure.class,remap=false)
public abstract class AltarPillarGateMixin {
 @Inject(method="isFeatureChunk",at=@At("RETURN"),cancellable=true)
 private void backport$exactDensity(ChunkGenerator generator,BiomeProvider provider,long seed,SharedSeedRandom random,int x,int z,Biome biome,ChunkPos chunk,NoFeatureConfig config,CallbackInfoReturnable<Boolean> ci){if(ci.getReturnValue()&&((HauntedTowerStructure)(Object)this).getRegistryName().equals(local.pixelmon.backport.SpearPillarFrequency.ID)&&!local.pixelmon.backport.AltarSpacing.pillarGate(seed,x,z))ci.setReturnValue(false);}
}
