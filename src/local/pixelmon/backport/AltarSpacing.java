package local.pixelmon.backport;
import java.nio.file.*;
import java.util.*;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraftforge.eventbus.api.SubscribeEvent;
/** Serialize reservations per dimension, including unloaded legacy structures read from region metadata. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Content.NS)
public final class AltarSpacing {
 public static final int DISTANCE=3000;
 public static final int SPAWN_DISTANCE=1000;
 public static final String AUTHOR="pixelmon_backport:author_gym";
 public static final Set<String> SPAWN_PROTECTED=Set.of("pixelmon:uno_shrine","pixelmon:dos_shrine","pixelmon:tres_shrine","pixelmon:ilex_shrine","pixelmon:spear_pillar",AUTHOR);
 public static final Set<String> TYPES=Set.of("pixelmon:uno_shrine","pixelmon:dos_shrine","pixelmon:tres_shrine","pixelmon:ilex_shrine","pixelmon:spear_pillar","pixelmon:land_chalice","pixelmon:water_chalice",AUTHOR);
 private static final Map<ChunkGenerator,Index> WORLDS=Collections.synchronizedMap(new WeakHashMap<>());
 public static boolean managed(String id){return TYPES.contains(id);}
 /** Nearest point of the building footprint must be outside the world-spawn circle. No chunk reads. */
 public static boolean outsideSpawn(ServerWorld world,int minX,int minZ,int maxX,int maxZ){
  var info=world.func_72912_H();long sx=info.func_76079_c(),sz=info.func_76074_e();
  long dx=sx-Math.max((long)minX,Math.min(sx,(long)maxX)),dz=sz-Math.max((long)minZ,Math.min(sz,(long)maxZ));
  return dx*dx+dz*dz>=(long)SPAWN_DISTANCE*SPAWN_DISTANCE;
 }
 public static boolean authorOutsideSpawn(ServerWorld world,net.minecraft.util.math.BlockPos home){return outsideSpawn(world,home.func_177958_n()-7,home.func_177952_p()-7,home.func_177958_n()+7,home.func_177952_p()+7);}
 public static double chance(double original){return Math.max(0,Math.min(1,original*10));}
 public static boolean pillarGate(long seed,int x,int z){return Math.floorMod(new Random(seed^((long)x*341873128712L)^((long)z*132897987541L)^82316132L).nextLong(),10)!=0;}
 @SubscribeEvent public static void load(net.minecraftforge.event.world.WorldEvent.Load e){if(e.getWorld() instanceof ServerWorld){ServerWorld w=(ServerWorld)e.getWorld();Path root=w.func_73046_m().func_240776_a_(net.minecraft.world.storage.FolderName.field_237253_i_);if(w.func_234923_W_().equals(World.field_234919_h_))root=root.resolve("DIM-1");else if(w.func_234923_W_().equals(World.field_234920_i_))root=root.resolve("DIM1");else if(!w.func_234923_W_().equals(World.field_234918_g_)){var id=w.func_234923_W_().func_240901_a_();root=root.resolve("dimensions").resolve(id.func_110624_b()).resolve(id.func_110623_a());}Index index=new Index(root.resolve("region"),w);for(var home:AuthorRoomIndex.get(w).positions(false))index.observe(new AltarRegionIndex.Point(AUTHOR,home.func_177958_n(),home.func_177952_p()));WORLDS.put(w.func_72863_F().func_201711_g(),index);}}
 @SubscribeEvent public static void unload(net.minecraftforge.event.world.WorldEvent.Unload e){if(e.getWorld() instanceof ServerWorld)WORLDS.remove(((ServerWorld)e.getWorld()).func_72863_F().func_201711_g());}
 @SubscribeEvent public static void chunk(net.minecraftforge.event.world.ChunkEvent.Load e){if(!(e.getWorld() instanceof ServerWorld))return;var w=(ServerWorld)e.getWorld();Index index=WORLDS.get(w.func_72863_F().func_201711_g());if(index!=null)for(var start:e.getChunk().func_201609_c().values())if(start.func_75069_d()&&managed(start.func_214627_k().getRegistryName().toString())){var box=start.func_75071_a();index.observe(new AltarRegionIndex.Point(start.func_214627_k().getRegistryName().toString(),(int)(((long)box.field_78897_a+box.field_78893_d)/2),(int)(((long)box.field_78896_c+box.field_78892_f)/2)));}}
 public static void generated(ChunkGenerator generator,StructureStart<?> start){if(!start.func_75069_d()||!managed(start.func_214627_k().getRegistryName().toString()))return;Index index=WORLDS.get(generator);if(index==null)return;var box=start.func_75071_a();if(!index.allows(start.func_214627_k().getRegistryName().toString(),box.field_78897_a,box.field_78896_c,box.field_78893_d,box.field_78892_f)||!index.reserve(start.func_214627_k().getRegistryName().toString(),(int)(((long)box.field_78897_a+box.field_78893_d)/2),(int)(((long)box.field_78896_c+box.field_78892_f)/2)))start.func_186161_c().clear();}
 public static boolean author(ServerWorld world,net.minecraft.util.math.BlockPos home){
  if(!authorOutsideSpawn(world,home))return false;
  Index index=WORLDS.get(world.func_72863_F().func_201711_g());
  if(index==null)throw new IllegalStateException("Author spacing index unavailable for dimension "+world.func_234923_W_());
  return index.reserve(AUTHOR,home.func_177958_n(),home.func_177952_p());
 }
 public static final class Index {
  private final Path region;private final Set<Long> scanned=new HashSet<>();private final Map<String,Map<Long,List<AltarRegionIndex.Point>>> points=new HashMap<>();
  private final ServerWorld world;
  public Index(Path region){this(region,null);}
  public Index(Path region,ServerWorld world){this.region=region;this.world=world;}
  private boolean allows(String type,int minX,int minZ,int maxX,int maxZ){return world==null||!SPAWN_PROTECTED.contains(type)||outsideSpawn(world,minX,minZ,maxX,maxZ);}
  private static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
  public synchronized void observe(AltarRegionIndex.Point p){var cell=points.computeIfAbsent(p.type,k->new HashMap<>()).computeIfAbsent(key(Math.floorDiv(p.x,DISTANCE),Math.floorDiv(p.z,DISTANCE)),k->new ArrayList<>());if(cell.stream().noneMatch(q->q.x==p.x&&q.z==p.z))cell.add(p);}
  public synchronized boolean reserve(String type,int x,int z){
   if(!managed(type))return true;
   for(int rx=Math.floorDiv(x-DISTANCE,512);rx<=Math.floorDiv(x+DISTANCE,512);rx++)for(int rz=Math.floorDiv(z-DISTANCE,512);rz<=Math.floorDiv(z+DISTANCE,512);rz++)if(scanned.add(key(rx,rz)))try{for(var p:AltarRegionIndex.read(region.resolve("r."+rx+"."+rz+".mca"),rx,rz))observe(p);}catch(java.io.IOException failure){scanned.remove(key(rx,rz));throw new IllegalStateException("Cannot read existing altar metadata: "+region,failure);}
   var buckets=points.getOrDefault(type,Collections.emptyMap());int cx=Math.floorDiv(x,DISTANCE),cz=Math.floorDiv(z,DISTANCE);for(int bx=cx-1;bx<=cx+1;bx++)for(int bz=cz-1;bz<=cz+1;bz++)for(var p:buckets.getOrDefault(key(bx,bz),Collections.emptyList())){long dx=(long)x-p.x,dz=(long)z-p.z;if(dx==0&&dz==0)return true;if(dx*dx+dz*dz<(long)DISTANCE*DISTANCE)return false;}
   observe(new AltarRegionIndex.Point(type,x,z));return true;
  }
 }
}
