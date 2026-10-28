package local.pixelmon.backport;
import net.minecraft.entity.Entity;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
/** Local room coordinates; legacy rooms have no tag and keep their original south doorway. */
public final class AuthorRoomRotation {
 public static final String TAG="BackportAuthorRoomRotation";
 private AuthorRoomRotation(){}
 public static Rotation get(Entity boss){return Rotation.values()[Math.floorMod(boss.getPersistentData().func_74762_e(TAG),4)];}
 public static BlockPos pos(BlockPos home,int x,int y,int z,Rotation r){
  switch(r){case CLOCKWISE_90:return home.func_177982_a(-z,y,x);case CLOCKWISE_180:return home.func_177982_a(-x,y,-z);case COUNTERCLOCKWISE_90:return home.func_177982_a(z,y,-x);default:return home.func_177982_a(x,y,z);}
 }
}
