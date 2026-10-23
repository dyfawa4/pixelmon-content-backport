package local.pixelmon.backport.mixin;

import java.util.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import local.pixelmon.backport.GymCities;
import local.pixelmon.backport.EndGymCities;
import local.pixelmon.backport.EndGymPlacement;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
    @Inject(method="generatePieces",at=@At("HEAD"),cancellable=true)
    private void backport$outerIslands(DynamicRegistries registries,ChunkGenerator generator,TemplateManager templates,int x,int z,net.minecraft.world.biome.Biome biome,GymConfig config,CallbackInfo ci){
        if(EndGymCities.psychic(config)&&!EndGymCities.eligible(generator,biome,x,z))ci.cancel();
    }
    @Redirect(method="generatePieces",at=@At(value="INVOKE",target="Lnet/minecraft/world/gen/feature/jigsaw/JigsawManager;func_242837_a(Lnet/minecraft/util/registry/DynamicRegistries;Lnet/minecraft/world/gen/feature/structure/VillageConfig;Lnet/minecraft/world/gen/feature/jigsaw/JigsawManager$IPieceFactory;Lnet/minecraft/world/gen/ChunkGenerator;Lnet/minecraft/world/gen/feature/template/TemplateManager;Lnet/minecraft/util/math/BlockPos;Ljava/util/List;Ljava/util/Random;ZZ)V"))
    private void backport$cityPool(DynamicRegistries registries,VillageConfig config,JigsawManager.IPieceFactory factory,ChunkGenerator generator,TemplateManager templates,BlockPos pos,List<net.minecraft.world.gen.feature.structure.AbstractVillagePiece> pieces,Random random,boolean expand,boolean project){
        GymConfig loaded=GymCities.config(registries,(GymConfig)config);
        if(EndGymCities.psychic(loaded))EndGymPlacement.assemble(registries,loaded,factory,generator,templates,pos,pieces,random,expand,project);
        else JigsawManager.func_242837_a(registries,loaded,factory,generator,templates,pos,pieces,random,expand,project);
    }
}
