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

@Mixin(value=LakesFeature.class,remap=false)
public abstract class GymLakeProtectionMixin {
    @Inject(method="func_241855_a(Lnet/minecraft/world/ISeedReader;Lnet/minecraft/world/gen/ChunkGenerator;Ljava/util/Random;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/feature/BlockStateFeatureConfig;)Z",at=@At("HEAD"),cancellable=true)
    private void backport$cityBeforeLake(ISeedReader world,ChunkGenerator generator,Random random,BlockPos pos,BlockStateFeatureConfig config,CallbackInfoReturnable<Boolean> cir){
        if(GymTerrainProtection.protectedArea(world,pos.func_177958_n(),pos.func_177952_p(),pos.func_177958_n()+15,pos.func_177952_p()+15))cir.setReturnValue(false);
    }
}
