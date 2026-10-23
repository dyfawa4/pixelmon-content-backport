package qa;
import java.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.gen.*;
import net.minecraft.world.gen.feature.structure.*;
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern;
import local.pixelmon.backport.EndGymCities;
public final class EndCityDiagnosis {
 public static String reason(ChunkGenerator gen,List<AbstractVillagePiece> pieces){try{
  var method=EndGymCities.class.getDeclaredMethod("foundation",StructurePiece.class);method.setAccessible(true);
  var projected=EndGymCities.class.getDeclaredMethod("projected",StructurePiece.class);projected.setAccessible(true);
  for(var p:pieces)if((boolean)method.invoke(null,p)&&!(boolean)projected.invoke(null,p)&&p.func_214828_c().func_177956_o()+p.func_214830_d()-1<16)return "rigid-low="+p;
  MutableBoundingBox b=MutableBoundingBox.func_78887_a();for(var p:pieces)b.func_78888_b(p.func_74874_b());
  int width=b.field_78893_d-b.field_78897_a+1,depth=b.field_78892_f-b.field_78896_c+1;if(width>512||depth>512)return "extent="+width+"x"+depth;
  int[] floors=new int[width*depth];Arrays.fill(floors,Integer.MIN_VALUE);String[] owner=new String[width*depth];boolean[] occupied=new boolean[width*depth];
  for(var p:pieces){var box=p.func_74874_b();int floor=p.func_214828_c().func_177956_o()+p.func_214830_d()-1;boolean foundation=(boolean)method.invoke(null,p)&&!(boolean)projected.invoke(null,p);
   for(int x=box.field_78897_a;x<=box.field_78893_d;x++)for(int z=box.field_78896_c;z<=box.field_78892_f;z++){int i=(x-b.field_78897_a)*depth+z-b.field_78896_c;occupied[i]=true;if(foundation&&floor>floors[i]){floors[i]=floor;owner[i]=p.toString();}else if(owner[i]==null)owner[i]=p.toString();}
  }
  for(int step:new int[]{8,1})for(int x=b.field_78897_a;x<=b.field_78893_d;x+=step)for(int z=b.field_78896_c;z<=b.field_78892_f;z+=step){int i=(x-b.field_78897_a)*depth+z-b.field_78896_c;if(!occupied[i])continue;int floor=floors[i],top=gen.func_222532_b(x,z,Heightmap.Type.WORLD_SURFACE_WG);var c=gen.func_230348_a_(x,z);boolean pass=false;
   if(floor==Integer.MIN_VALUE){floor=top;pass=top>=18&&top<=200;}else for(int drop=1;drop<=5;drop++)if(c.func_180495_p(new BlockPos(x,floor-drop,z)).func_185904_a().func_76220_a()&&c.func_180495_p(new BlockPos(x,floor-drop-1,z)).func_185904_a().func_76220_a()){pass=true;break;}
   if(!pass){String solids="";for(int yy=Math.max(0,top-12);yy<top+2;yy++)if(c.func_180495_p(new BlockPos(x,yy,z)).func_185904_a().func_76220_a())solids+=yy+",";return "solidY="+solids+" point="+x+","+z+" floor="+floor+" surface="+top+" piece="+owner[i];}
  }
  return "terrain-supported";
 }catch(Exception e){return "diagnostic="+e;}}
}
