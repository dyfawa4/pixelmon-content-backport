package probe;
import java.util.*;
import qa.EndCityDiagnosis;
import com.pixelmonmod.pixelmon.world.structure.type.GymConfig;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.gen.*;
import net.minecraft.world.gen.feature.jigsaw.JigsawManager;
import net.minecraft.world.gen.feature.structure.*;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.util.math.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import local.pixelmon.backport.*;
@Mixin(value=EndGymPlacement.class,remap=false)
public class EndPlacementTraceMixin {
 @Redirect(method="assemble",at=@At(value="INVOKE",target="Lnet/minecraft/world/gen/feature/jigsaw/JigsawManager;func_242837_a(Lnet/minecraft/util/registry/DynamicRegistries;Lnet/minecraft/world/gen/feature/structure/VillageConfig;Lnet/minecraft/world/gen/feature/jigsaw/JigsawManager$IPieceFactory;Lnet/minecraft/world/gen/ChunkGenerator;Lnet/minecraft/world/gen/feature/template/TemplateManager;Lnet/minecraft/util/math/BlockPos;Ljava/util/List;Ljava/util/Random;ZZ)V"))
 private static void trace(DynamicRegistries r,VillageConfig c,JigsawManager.IPieceFactory f,ChunkGenerator g,TemplateManager t,BlockPos pos,List<AbstractVillagePiece> p,Random random,boolean expand,boolean project){
  JigsawManager.func_242837_a(r,c,f,g,t,pos,p,random,expand,project);
  int floor=p.stream().mapToInt(q->q.func_214828_c().func_177956_o()+q.func_214830_d()-1).min().orElse(-999);
  System.out.println("FULL_TRIAL pos="+pos+" pieces="+p.size()+" floor="+floor+" houses="+EndGymCities.houses(p)+" roads="+EndGymCities.roads(p)+" district="+EndGymCities.district(p)+" reason="+EndCityDiagnosis.reason(g,p));
 }
}
