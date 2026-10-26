package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.world.structure.type.ShrineStructure;
import com.pixelmonmod.pixelmon.api.config.StructureConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=ShrineStructure.class,remap=false)
public abstract class AltarChanceMixin {
 @Redirect(method="isFeatureChunk",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/config/StructureConfig;getIceShrineSpawnRates()D"))
 private double backport$tenfold(StructureConfig config){return local.pixelmon.backport.AltarSpacing.chance(config.getIceShrineSpawnRates());}
}
