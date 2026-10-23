package local.pixelmon.backport;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.event.TickEvent;
/** Backfill saved, recognized jigsaw buildings as their chunks load; never search or generate chunks. */
public final class CenterMusic {
 public static boolean centerTrack(ResourceLocation track){return new ResourceLocation("pixelmon:pixelmon.music.pokecenter").equals(track);}
 public static BlockPos limit(ResourceLocation track,BlockPos size){if(!centerTrack(track)||size==null||size.func_177958_n()<=6&&size.func_177956_o()<=8&&size.func_177952_p()<=6)return size;return new BlockPos(Math.min(6,size.func_177958_n()),Math.min(8,size.func_177956_o()),Math.min(6,size.func_177952_p()));}
 private static final com.google.gson.JsonObject TEMPLATES=Content.read("backport/center_music_templates.json");
 private final Queue<Chunk> chunks=new ConcurrentLinkedQueue<>();
 private static final class Placement {final ServerWorld world;final BlockPos pos,size;Placement(ServerWorld w,BlockPos p,BlockPos s){world=w;pos=p;size=s;}}
 private final Queue<Placement> waiting=new ArrayDeque<>();
 @SubscribeEvent public void load(ChunkEvent.Load event){if(event.getWorld() instanceof ServerWorld&&event.getChunk() instanceof Chunk)chunks.add((Chunk)event.getChunk());}
 public static BlockPos position(CompoundNBT piece,BlockPos local){
  Rotation rotation;try{rotation=Rotation.valueOf(piece.func_74779_i("rotation"));}catch(IllegalArgumentException bad){rotation=Rotation.NONE;}
  return Template.func_207669_a(local,Mirror.NONE,rotation,BlockPos.field_177992_a).func_177982_a(piece.func_74762_e("PosX"),piece.func_74762_e("PosY"),piece.func_74762_e("PosZ"));
 }
 private static BlockPos point(com.google.gson.JsonArray a){return new BlockPos(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());}
 private void inspect(Chunk chunk){
  if(!(chunk.func_177412_p() instanceof ServerWorld))return;var world=(ServerWorld)chunk.func_177412_p();
  for(var start:chunk.func_201609_c().values())for(var piece:start.func_186161_c()){
   restorePiece(world,piece.func_143010_b());
  }
 }
 public void restorePiece(ServerWorld world,CompoundNBT piece){
  String id=piece.func_74775_l("pool_element").func_74779_i("location");var value=TEMPLATES.get(id);if(value==null)return;
  var data=value.getAsJsonObject();waiting.add(new Placement(world,position(piece,point(data.getAsJsonArray("position"))),point(data.getAsJsonArray("size"))));
 }
 public static boolean install(ServerWorld world,BlockPos pos,BlockPos size){
  if(!world.func_175623_d(pos))return false;
  var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pixelmon:music_block"));if(block==null)return false;
  var state=block.func_176223_P();world.func_180501_a(pos,state,3);var tile=world.func_175625_s(pos);if(tile==null)throw new IllegalStateException("Native music tile missing");
  var tag=tile.func_189515_b(new CompoundNBT());tag.func_74778_a("track","pixelmon:pixelmon.music.pokecenter");tag.func_74757_a("BackportAmbient",true);
  for(String axis:List.of("X","Y","Z"))tag.func_74768_a("offset"+axis,0);
  tag.func_74768_a("sizeX",size.func_177958_n());tag.func_74768_a("sizeY",size.func_177956_o());tag.func_74768_a("sizeZ",size.func_177952_p());
  tile.func_230337_a_(state,tag);tile.func_70296_d();world.func_184138_a(pos,state,state,3);return true;
 }
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
  if(event.phase!=TickEvent.Phase.END)return;
  for(int i=0;i<4;i++){var chunk=chunks.poll();if(chunk==null)break;inspect(chunk);}
  int budget=Math.min(16,waiting.size());for(int i=0;i<budget;i++){var next=waiting.remove();var p=next.pos;if(next.world.func_72863_F().func_225313_a(p.func_177958_n()>>4,p.func_177952_p()>>4)==null){waiting.add(next);continue;}install(next.world,p,next.size);}
 }
 @SubscribeEvent public void unload(WorldEvent.Unload event){waiting.removeIf(p->p.world==event.getWorld());chunks.removeIf(c->c.func_177412_p()==event.getWorld());}
}
