package local.pixelmon.backport.mixin;

import local.pixelmon.backport.UltraPlantAchievement;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=LightningBoltEntity.class,remap=false)
public abstract class UltraPlantLightningHitMixin {
    @Redirect(method="func_70071_h_",at=@At(value="INVOKE",target="Lnet/minecraft/entity/Entity;func_241841_a(Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/entity/effect/LightningBoltEntity;)V"))
    private void backport$afterActualStrike(Entity target,ServerWorld world,LightningBoltEntity bolt){
        target.func_241841_a(world,bolt);
        UltraPlantAchievement.struck(target,bolt);
    }
}
