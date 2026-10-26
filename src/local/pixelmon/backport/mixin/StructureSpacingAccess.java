package local.pixelmon.backport.mixin;
import java.util.Map;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.settings.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=DimensionStructuresSettings.class,remap=false)
public interface StructureSpacingAccess {
    @Mutable @Accessor("field_236193_d_") void backport$structureSettings(Map<Structure<?>,StructureSeparationSettings> settings);
}
