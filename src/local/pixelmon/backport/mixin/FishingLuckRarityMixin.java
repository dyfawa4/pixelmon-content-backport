package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.spawning.*;
import local.pixelmon.backport.FishingEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=SpawnInfo.class,remap=false)
public abstract class FishingLuckRarityMixin {
    @Inject(method="getAdjustedRarity",at=@At("RETURN"),cancellable=true)
    private void backport$rareBite(AbstractSpawner spawner,SpawnLocation location,CallbackInfoReturnable<Float> ci){ci.setReturnValue(FishingEnchantments.rarity((SpawnInfo)(Object)this,spawner,location,ci.getReturnValue()));}
}
