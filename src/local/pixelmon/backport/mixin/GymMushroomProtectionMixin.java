package local.pixelmon.backport.mixin;

import local.pixelmon.backport.GymTerrainProtection;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.AbstractBigMushroomFeature;
import net.minecraft.world.gen.feature.BigMushroomFeatureConfig;
import net.minecraft.util.math.BlockPos;
import java.util.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Both giant mushroom types have their own placement path, separate from TreeFeature. */
@Mixin(value=AbstractBigMushroomFeature.class,remap=false)
public abstract class GymMushroomProtectionMixin {
    @Inject(method="func_241855_a(Lnet/minecraft/world/ISeedReader;Lnet/minecraft/world/gen/ChunkGenerator;Ljava/util/Random;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/feature/BigMushroomFeatureConfig;)Z",at=@At("HEAD"),cancellable=true)
    private void backport$cityBeforeMushroom(ISeedReader world,ChunkGenerator generator,Random random,BlockPos pos,BigMushroomFeatureConfig config,CallbackInfoReturnable<Boolean> cir){
        int radius=Math.max(0,config.field_227274_c_);
        if(GymTerrainProtection.protectedArea(world,pos.func_177958_n()-radius,pos.func_177952_p()-radius,pos.func_177958_n()+radius,pos.func_177952_p()+radius))cir.setReturnValue(false);
    }
}
