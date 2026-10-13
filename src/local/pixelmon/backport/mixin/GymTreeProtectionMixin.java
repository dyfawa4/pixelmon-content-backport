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

@Mixin(value=TreeFeature.class,remap=false)
public abstract class GymTreeProtectionMixin {
    @Inject(method="func_241855_a(Lnet/minecraft/world/ISeedReader;Lnet/minecraft/world/gen/ChunkGenerator;Ljava/util/Random;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/feature/BaseTreeFeatureConfig;)Z",at=@At("HEAD"),cancellable=true)
    private void backport$cityBeforeTree(ISeedReader world,ChunkGenerator generator,Random random,BlockPos pos,BaseTreeFeatureConfig config,CallbackInfoReturnable<Boolean> cir){
        if(GymTerrainProtection.protectedArea(world,pos.func_177958_n()-16,pos.func_177952_p()-16,pos.func_177958_n()+16,pos.func_177952_p()+16))cir.setReturnValue(false);
    }
}
