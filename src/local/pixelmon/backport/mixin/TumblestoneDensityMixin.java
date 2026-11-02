package local.pixelmon.backport.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Scale only the native occurrence gate, preserving terrain and colour selection. */
@Mixin(targets="com.pixelmonmod.pixelmon.world.generation.features.TumblestoneFeature",remap=false)
public abstract class TumblestoneDensityMixin {
 @ModifyArg(method="place(Lnet/minecraft/world/ISeedReader;Lnet/minecraft/world/gen/ChunkGenerator;Ljava/util/Random;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/feature/NoFeatureConfig;)Z",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/util/helpers/RandomHelper;getRandomChance(F)Z"),index=0,require=1)
 private float backport$tenfold(float chance){return Math.min(1f,Math.max(0f,chance)*10f);}
}
