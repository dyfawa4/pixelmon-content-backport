package local.pixelmon.backport.mixin;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.biome.Biome;
import net.minecraft.util.registry.DynamicRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets={"com.pixelmonmod.pixelmon.world.structure.type.ShrineStructure$Start","com.pixelmonmod.pixelmon.world.structure.type.HauntedTowerStructure$Start","com.pixelmonmod.pixelmon.world.structure.type.BasicStructure$Start"},remap=false)
public abstract class AltarSpacingMixin {
 @Inject(method="generatePieces",at=@At("RETURN"))
 private void backport$spacing(DynamicRegistries registries,ChunkGenerator generator,TemplateManager templates,int x,int z,Biome biome,NoFeatureConfig config,CallbackInfo ci){local.pixelmon.backport.AltarSpacing.generated(generator,(StructureStart<?>)(Object)this);}
}
