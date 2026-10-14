package local.pixelmon.backport.mixin;
import java.util.List;
import local.pixelmon.backport.NaturalPokeStops;
import net.minecraft.world.IWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.template.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=Template.class,remap=false)
public abstract class NaturalPokeStopMixin {
    @Inject(method="func_237145_a_",at=@At("RETURN"),cancellable=true)
    private static void backport$naturalStops(IWorld world,BlockPos origin,BlockPos pivot,PlacementSettings settings,List<Template.BlockInfo> blocks,CallbackInfoReturnable<List<Template.BlockInfo>> cir){
        cir.setReturnValue(NaturalPokeStops.convert(world,cir.getReturnValue()));
    }
}
