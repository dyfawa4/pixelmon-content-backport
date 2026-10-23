package local.pixelmon.backport.mixin;
import local.pixelmon.backport.GymTerrainProtection;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.*;
import net.minecraft.util.math.BlockPos;
import java.util.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ChorusPlantFeature.class,remap=false)
public abstract class GymChorusProtectionMixin {
    @Inject(method="func_241855_a(Lnet/minecraft/world/ISeedReader;Lnet/minecraft/world/gen/ChunkGenerator;Ljava/util/Random;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/feature/NoFeatureConfig;)Z",at=@At("HEAD"),cancellable=true)
    private void backport$cityBeforeChorus(ISeedReader world,ChunkGenerator generator,Random random,BlockPos pos,NoFeatureConfig config,CallbackInfoReturnable<Boolean> cir){
        // Native chorus branches may extend eight blocks from their seed.
        if(GymTerrainProtection.protectedArea(world,pos.func_177958_n()-8,pos.func_177952_p()-8,pos.func_177958_n()+8,pos.func_177952_p()+8))cir.setReturnValue(false);
    }
}
