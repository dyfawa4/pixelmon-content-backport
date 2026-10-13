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

@Mixin(value=RandomPatchFeature.class,remap=false)
public abstract class GymPatchProtectionMixin {
    @Inject(method="func_241855_a(Lnet/minecraft/world/ISeedReader;Lnet/minecraft/world/gen/ChunkGenerator;Ljava/util/Random;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/feature/BlockClusterFeatureConfig;)Z",at=@At("HEAD"),cancellable=true)
    private void backport$cityBeforePatch(ISeedReader world,ChunkGenerator generator,Random random,BlockPos pos,BlockClusterFeatureConfig config,CallbackInfoReturnable<Boolean> cir){
        int x=Math.max(0,config.field_227294_g_),z=Math.max(0,config.field_227296_i_);
        if(GymTerrainProtection.protectedArea(world,pos.func_177958_n()-x,pos.func_177952_p()-z,pos.func_177958_n()+x,pos.func_177952_p()+z))cir.setReturnValue(false);
    }
}
