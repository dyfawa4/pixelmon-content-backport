import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import local.pixelmon.backport.TeraRaids;

/** Sample the production conversion method, including structured chunk coordinates. */
public final class DenDistributionVerification {
 public static void main(String[] args)throws Exception{
  JsonArray rows=new JsonArray();long totalTera=0,total=0;
  String[] dimensions=args.length==0?new String[]{"minecraft:overworld","minecraft:the_nether","minecraft:the_end"}:new String[]{args[0]};
  for(String dimension:dimensions)
   for(long seed:new long[]{0,1,21,143143,-1,Long.MIN_VALUE,Long.MAX_VALUE,20261015})
    for(boolean grid:new boolean[]{false,true}){
     Random random=new Random(seed^0x243f6a8885a308d3L);int tera=0,n=100000;
     for(int i=0;i<n;i++){
      int x,z,y;if(grid){x=(i%400-200)*16+7;z=(i/400-125)*16+7;y=64;}
      else{x=random.nextInt(60000001)-30000000;z=random.nextInt(60000001)-30000000;y=random.nextInt(256);}
      boolean selected=TeraRaids.convert(seed,dimension,x,y,z);
      if(selected!=TeraRaids.convert(seed,dimension,x,y,z))throw new AssertionError("Reload changes den kind");
      if(selected)tera++;
     }
     if(tera<48000||tera>52000)throw new AssertionError("Biased conversion: "+dimension+" seed="+seed+" grid="+grid+" tera="+tera);
     JsonObject row=new JsonObject();row.addProperty("dimension",dimension);row.addProperty("seed",seed);row.addProperty("grid",grid);row.addProperty("tera",tera);row.addProperty("max",n-tera);rows.add(row);totalTera+=tera;total+=n;
    }
  JsonObject report=new JsonObject();report.addProperty("method","production TeraRaids.convert; not a natural-world population census");report.addProperty("total",total);report.addProperty("tera",totalTera);report.addProperty("max",total-totalTera);report.addProperty("teraPercent",100.0*totalTera/total);report.add("cases",rows);
  Files.writeString(Path.of(args.length==0?".cache/den-conversion-distribution.json":".cache/den-conversion-overworld.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
  System.out.println("PASS production den conversion dimensions="+Arrays.toString(dimensions)+" samples="+total+" tera="+totalTera+" max="+(total-totalTera)+" teraPercent="+(100.0*totalTera/total)+"; "+rows.size()+" seed/dimension/lattice cases, deterministic reload classification. Natural-world and detector counts are separate checks.");
 }
}
