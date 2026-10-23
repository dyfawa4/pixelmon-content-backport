package probe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.template.TemplateManager;
import com.pixelmonmod.pixelmon.world.structure.type.GymConfig;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.*;
import local.pixelmon.backport.EndGymCities;
@Mixin(targets="com.pixelmonmod.pixelmon.world.structure.type.GymStructure$Start",remap=false)
public class EndLayoutTraceMixin {
 @Inject(method="generatePieces",at=@At("RETURN"))
 private void trace(DynamicRegistries r,ChunkGenerator gen,TemplateManager t,int x,int z,Biome b,GymConfig c,CallbackInfo ci){
  if(!EndGymCities.psychic(c))return;var p=((StructureStart<?>)(Object)this).func_186161_c();
  int floor=p.stream().filter(q->q instanceof AbstractVillagePiece).mapToInt(q->((AbstractVillagePiece)q).func_214828_c().func_177956_o()+((AbstractVillagePiece)q).func_214830_d()-1).min().orElse(-999);
  boolean main=p.stream().anyMatch(q->q.toString().contains("/gym_psychic]"));
  System.out.println("END_LAYOUT x="+x+" z="+z+" n="+p.size()+" floor="+floor+" main="+main+" houses="+EndGymCities.houses(p)+" roads="+EndGymCities.roads(p)+" supported="+EndGymCities.complete(gen,p));
 }
}
