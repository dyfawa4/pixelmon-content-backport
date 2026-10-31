package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.blocks.daycare.DayCareBlock;
import net.minecraft.world.IBlockReader;
import net.minecraft.util.math.*;

/** Legacy upper-cell selection must never steal a real block's interaction. */
public final class DayCareSelection {
 private DayCareSelection(){}
 public static BlockRayTraceResult backport$upper(IBlockReader world,RayTraceContext context,BlockPos cell,BlockRayTraceResult original){
  var cellState=world.func_180495_p(cell);
  if(!cellState.func_196958_f()&&!DayCareFootprint.upper(cellState)&&!(cellState.func_177230_c() instanceof TumblestoneFootprint.Upper))return original;
  BlockPos base=cell.func_177977_b();var state=world.func_180495_p(base);
  if(!(state.func_177230_c() instanceof DayCareBlock)&&!(state.func_177230_c() instanceof com.pixelmonmod.pixelmon.blocks.multi.FridgeBlock)&&!TumblestoneFootprint.tall(state))return original;
  var start=context.func_222253_b();var end=context.func_222250_a();
  var hit=context.func_222251_a(state,world,base).func_212433_a(start,end,base);
  if(hit==null)return original;
  // Keep the cell the player actually aimed at. Upper blocks delegate opening/breaking
  // to their base, but BlockItem must place beside the upper cell, not the base.
  if(hit.func_216347_e().field_72448_b>=base.func_177956_o()+1)hit=new BlockRayTraceResult(hit.func_216347_e(),hit.func_216354_b(),cell,hit.func_216353_d());
  return original==null||start.func_72436_e(hit.func_216347_e())<=start.func_72436_e(original.func_216347_e())?hit:original;
 }
}
