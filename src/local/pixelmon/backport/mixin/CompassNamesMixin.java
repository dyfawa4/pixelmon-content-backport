package local.pixelmon.backport.mixin;
import local.pixelmon.backport.CompassNames;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.feature.structure.Structure;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets="com.chaosthedude.explorerscompass.util.StructureUtils",remap=false)
public abstract class CompassNamesMixin {
    @Shadow public static ResourceLocation getKeyForStructure(Structure<?> structure){throw new AssertionError();}
    @Inject(method="getStructureName",at=@At("HEAD"),cancellable=true)
    private static void backport$structureName(Structure<?> structure,CallbackInfoReturnable<String> ci){
        if(structure==null)return;String value=CompassNames.name("structure",getKeyForStructure(structure));if(value!=null)ci.setReturnValue(value);
    }
    @Inject(method="getDimensionName",at=@At("HEAD"),cancellable=true)
    private static void backport$dimensionName(ResourceLocation id,CallbackInfoReturnable<String> ci){
        String value=CompassNames.name("dimension",id);if(value!=null)ci.setReturnValue(value);
    }
    @Inject(method="getStructureSource",at=@At("HEAD"),cancellable=true)
    private static void backport$structureSource(Structure<?> structure,CallbackInfoReturnable<String> ci){
        if(structure==null)return;String value=CompassNames.name("source",getKeyForStructure(structure));if(value!=null)ci.setReturnValue(value);
    }
}
