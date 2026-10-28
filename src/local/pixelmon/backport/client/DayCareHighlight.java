package local.pixelmon.backport.client;
import local.pixelmon.backport.DayCareFootprint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.*;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.DrawHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Draw one two-cell outline without changing physical/lighting shapes or the actual hit target. */
@Mod.EventBusSubscriber(modid="pixelmon_backport",value=Dist.CLIENT)
public final class DayCareHighlight {
 public static final VoxelShape OUTLINE=VoxelShapes.func_197873_a(0,0,0,1,2,1);
 public static BlockPos base(IBlockReader world,BlockPos hit){
  var state=world.func_180495_p(hit);if(DayCareFootprint.lower(state))return hit;
  return DayCareFootprint.upper(state)&&DayCareFootprint.lower(world.func_180495_p(hit.func_177977_b()))?hit.func_177977_b():null;
 }
 @SubscribeEvent public static void draw(DrawHighlightEvent.HighlightBlock event){
  var world=Minecraft.func_71410_x().field_71441_e;if(world==null)return;
  BlockPos base=base(world,event.getTarget().func_216350_a());if(base==null)return;
  var camera=event.getInfo().func_216785_c();
  WorldRenderer.func_228431_a_(event.getMatrix(),event.getBuffers().getBuffer(RenderType.func_228659_m_()),OUTLINE,
   base.func_177958_n()-camera.field_72450_a,base.func_177956_o()-camera.field_72448_b,base.func_177952_p()-camera.field_72449_c,0,0,0,.4f);
  event.setCanceled(true);
 }
}
