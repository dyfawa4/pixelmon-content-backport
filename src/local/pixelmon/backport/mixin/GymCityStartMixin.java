package local.pixelmon.backport.mixin;

import java.util.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import local.pixelmon.backport.GymCities;
import com.pixelmonmod.pixelmon.world.structure.type.GymConfig;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.jigsaw.JigsawManager;
import net.minecraft.world.gen.feature.structure.VillageConfig;
import net.minecraft.world.gen.feature.template.TemplateManager;

/** Resolve native hard-coded start pools through the loaded world's data packs. */
@Mixin(targets="com.pixelmonmod.pixelmon.world.structure.type.GymStructure$Start",remap=false)
public abstract class GymCityStartMixin {
    @ModifyArg(method="generatePieces",at=@At(value="INVOKE",target="Lnet/minecraft/world/gen/feature/jigsaw/JigsawManager;func_242837_a(Lnet/minecraft/util/registry/DynamicRegistries;Lnet/minecraft/world/gen/feature/structure/VillageConfig;Lnet/minecraft/world/gen/feature/jigsaw/JigsawManager$IPieceFactory;Lnet/minecraft/world/gen/ChunkGenerator;Lnet/minecraft/world/gen/feature/template/TemplateManager;Lnet/minecraft/util/math/BlockPos;Ljava/util/List;Ljava/util/Random;ZZ)V"),index=1)
    private VillageConfig backport$cityPool(DynamicRegistries registries,VillageConfig config,JigsawManager.IPieceFactory factory,ChunkGenerator generator,TemplateManager templates,BlockPos pos,List<?> pieces,Random random,boolean expand,boolean project){
        return GymCities.config(registries,(GymConfig)config);
    }
}
