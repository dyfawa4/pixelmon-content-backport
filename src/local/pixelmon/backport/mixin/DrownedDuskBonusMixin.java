package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.logic.DuskBall;
import com.pixelmonmod.pixelmon.world.dimension.drowned.DrownedWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Reuse the native night calculation, including its Ultra Beast penalty. */
@Mixin(value=DuskBall.class,remap=false)
public abstract class DrownedDuskBonusMixin {
    @Redirect(method="getCatchBonus",at=@At(value="INVOKE",target="Lnet/minecraft/world/World;func_72935_r()Z"))
    private boolean backport$nightInDrowned(World world){return !DrownedWorld.is(world) && world.func_72935_r();}
}
