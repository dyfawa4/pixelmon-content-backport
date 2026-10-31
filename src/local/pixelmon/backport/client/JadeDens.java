package local.pixelmon.backport.client;
import com.pixelmonmod.pixelmon.entities.DenEntity;
import local.pixelmon.backport.TeraDenEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
/** Jade-only model picking. Does not change combat collisions or click interaction. */
public final class JadeDens {
 public static RayTraceResult target(Entity viewer,double reach,float partial,RayTraceResult original){
  Vector3d start=viewer.func_174824_e(partial),end=start.func_178787_e(viewer.func_70676_i(partial).func_186678_a(reach));
  double best=original==null||original.func_216346_c()==RayTraceResult.Type.MISS?reach*reach:start.func_72436_e(original.func_216347_e());RayTraceResult result=original;
  for(Entity candidate:viewer.field_70170_p.func_175674_a(viewer,new AxisAlignedBB(start,end).func_186662_g(2),e->e instanceof DenEntity&&e.func_70089_S())){
   if(candidate==viewer.func_184187_bx())continue;
   double x=candidate.func_226277_ct_(),y=candidate.func_226278_cu_(),z=candidate.func_226281_cx_();
   AxisAlignedBB box=candidate instanceof TeraDenEntity?new AxisAlignedBB(x-.7,y,z-.7,x+.7,y+2.1,z+.7):new AxisAlignedBB(x-.9,y,z-.9,x+.9,y+.8,z+.9);
   var hit=box.func_216365_b(start,end);if(hit.isEmpty())continue;double distance=start.func_72436_e(hit.get());
   if(distance<best){best=distance;result=new EntityRayTraceResult(candidate,hit.get());}
  }
  return result;
 }
}
