package local.pixelmon.backport.mixin;

import java.util.*;
import local.pixelmon.backport.GymLocator;
import local.pixelmon.backport.TeraDenLocator;
import local.pixelmon.backport.StrongholdLocator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.chaosthedude.explorerscompass.util.StructureUtils",remap=false)
public abstract class CompassGymDimensionsMixin {
    @Inject(method="getStructureDimensions",at=@At("HEAD"),cancellable=true)
    private static void backport$gymDimensions(ServerWorld serverWorld,Structure<?> requested,CallbackInfoReturnable<List<ResourceLocation>> ci){
        if(!(requested instanceof GymLocator.TypedGym) && !(requested instanceof TeraDenLocator.TypedDen) && !StrongholdLocator.handles(requested))return;
        List<ResourceLocation> dimensions=new ArrayList<>();
        for(ServerWorld world:serverWorld.func_73046_m().func_212370_w())if(StrongholdLocator.handles(requested)?StrongholdLocator.available(world):requested instanceof TeraDenLocator.TypedDen?TeraDenLocator.available(world,(TeraDenLocator.TypedDen)requested):GymLocator.available(world,((GymLocator.TypedGym)requested).type))dimensions.add(world.func_234923_W_().func_240901_a_());
        ci.setReturnValue(dimensions);
    }
}
