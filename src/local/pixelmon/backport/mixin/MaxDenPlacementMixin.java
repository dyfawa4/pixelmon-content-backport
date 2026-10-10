package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.CommonProxy;
import com.pixelmonmod.pixelmon.items.SpawnDenItem;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Explicit native placement remains a Max den; only unclassified world dens convert. */
@Mixin(value=SpawnDenItem.class,remap=false)
public abstract class MaxDenPlacementMixin {
    @Redirect(method="func_77659_a",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/CommonProxy;spawnEntitySafely(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;)V"))
    private void backport$maxDen(Entity den,World world){den.getPersistentData().func_74768_a("BackportDenKind",0);CommonProxy.spawnEntitySafely(den,world);}
}
