package qa;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.io.*;
import java.util.zip.*;
import com.google.gson.*;
import local.pixelmon.backport.*;
import net.minecraft.block.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.structure.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Actual registry, structure start, face visibility, container and furnace checks in a private world. */
public final class Final190Cases {
 static int checks;static JsonObject report=new JsonObject();
 static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static BlockState block(String id){Block b=ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));check(b!=null&&b!=Blocks.field_150350_a,"Native block present "+id);return b.func_176223_P();}
 static ItemStack item(String id){return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));}
 static void index()throws Exception{
  Path none=Path.of("missing-private-regions");
  for(String type:AltarSpacing.TYPES){
   var i=new AltarSpacing.Index(none);check(i.reserve(type,-4096,-8192),"Initial reservation "+type);
   check(!i.reserve(type,-4096+2999,-8192),"2999 same type rejected "+type);
   check(i.reserve(type,-4096+3000,-8192),"3000 boundary allowed "+type);
   check(!i.reserve(type,-4096+2121,-8192+2121),"Horizontal diagonal below 3000 rejected "+type);
   check(i.reserve("pixelmon:unmanaged",-4095,-8191),"Unmanaged unaffected");
  }
  var mixed=new AltarSpacing.Index(none);for(String type:AltarSpacing.TYPES)check(mixed.reserve(type,0,0),"Different types independently allowed "+type);
  var parallel=new AltarSpacing.Index(none);var pool=Executors.newFixedThreadPool(4);List<Future<Boolean>> tasks=new ArrayList<>();
  try{for(int n=0;n<16;n++){final int x=n+1;tasks.add(pool.submit(()->parallel.reserve("pixelmon:uno_shrine",x,20)));}int accepted=0;for(var f:tasks)if(f.get())accepted++;check(accepted==1,"Concurrent reservations cannot generate two nearby starts");}finally{pool.shutdownNow();}
  check(new AltarSpacing.Index(none).reserve("pixelmon:uno_shrine",1,20),"Independent dimension index unaffected");
  int candidates=0;for(int x=0;x<128;x++)for(int z=0;z<128;z++){boolean ocean=AuthorBossPolicy.candidate(190027,x,z,false,"ocean");check(ocean==AuthorBossPolicy.candidate(190027,x,z,false,"forest"),"Ocean has no extra generation reduction");if(ocean)candidates++;}check(candidates==8,"Both author candidates retained per 64 chunk region");
 }
 @SuppressWarnings({"rawtypes","unchecked"}) static StructureStart<?> start(ServerWorld w,Structure structure,int x,int z){
  StructureStart s=structure.func_214557_a().create(structure,x,z,MutableBoundingBox.func_78887_a(),0,w.func_72905_C());
  s.func_230364_a_(w.func_241828_r(),w.func_72863_F().func_201711_g(),w.func_184163_y(),x,z,w.func_226691_t_(new BlockPos(0,64,0)),NoFeatureConfig.field_236559_b_);return s;
 }
 static void structures(ServerWorld w)throws Exception{
  JsonObject types=new JsonObject();StructureStart<?> legacy=null;int n=0;
  for(String type:new TreeSet<>(AltarSpacing.TYPES)){
   if(type.equals(AltarSpacing.AUTHOR))continue;Structure<?> structure=ForgeRegistries.STRUCTURE_FEATURES.getValue(new ResourceLocation(type));check(structure!=null,"Actual registered structure "+type);
   int cx=10000+n++*2000;var first=start(w,structure,cx,10000);check(first.func_75069_d(),"Actual native first start valid "+type+" "+structure.getClass());
   var second=start(w,structure,cx+1,10000);check(!second.func_75069_d(),"Actual native start rejected within3000 "+type);
   var distant=start(w,structure,cx+300,10000);check(distant.func_75069_d(),"Actual native distant start retained "+type);types.addProperty(type,structure.getClass().getSimpleName());if(type.equals("pixelmon:uno_shrine"))legacy=first;
  }
  check(legacy!=null,"Native saved shrine available");var old=legacy.func_143021_a(legacy.func_143019_e(),legacy.func_143018_f());
  var root=new CompoundNBT();var level=new CompoundNBT();var structures=new CompoundNBT();var starts=new CompoundNBT();starts.func_218657_a("pixelmon:uno_shrine",old);structures.func_218657_a("Starts",starts);level.func_218657_a("Structures",structures);root.func_218657_a("Level",level);
  int cx=legacy.func_143019_e(),cz=legacy.func_143018_f(),rx=Math.floorDiv(cx,32),rz=Math.floorDiv(cz,32);var bytes=new ByteArrayOutputStream();try(var zip=new DataOutputStream(new DeflaterOutputStream(bytes))){CompressedStreamTools.func_74800_a(root,zip);}byte[] payload=bytes.toByteArray();Path dir=Path.of("legacy-region-fixture");Files.createDirectories(dir);Path file=dir.resolve("r."+rx+"."+rz+".mca");
  try(var out=new RandomAccessFile(file.toFile(),"rw")){out.setLength(12288);out.seek(4L*(Math.floorMod(cx,32)+Math.floorMod(cz,32)*32));out.writeInt((2<<8)|1);out.seek(8192);out.writeInt(payload.length+1);out.writeByte(2);out.write(payload);}
  var points=AltarRegionIndex.read(file,rx,rz);check(points.size()==1&&points.get(0).type.equals("pixelmon:uno_shrine"),"Unloaded legacy native saved start parsed from region without chunk requests");var p=points.get(0);check(!new AltarSpacing.Index(dir).reserve(p.type,p.x+2999,p.z),"Unloaded legacy start blocks nearby new structure");
  report.add("nativeStructures",types);
 }
 static void faces(ServerWorld w){
  JsonArray kinds=new JsonArray();int n=0;
  for(String id:List.of("pixelmon:red_tumblestone","pixelmon:blue_tumblestone","pixelmon:black_tumblestone","pixelmon:rainbow_tumblestone","pixelmon:red_day_care","pixelmon:blue_day_care","pixelmon:lime_day_care")){
   BlockPos pos=new BlockPos(10+n++*4,90,10);w.func_175726_f(pos);var state=block(id);
   if(state.func_177230_c() instanceof com.pixelmonmod.pixelmon.blocks.TumblestoneBlock)state=state.func_206870_a(com.pixelmonmod.pixelmon.blocks.TumblestoneBlock.AGE,5);
   w.func_180501_a(pos,state,3);if(DayCareFootprint.lower(state))DayCareFootprint.reserve(w,pos);else TumblestoneFootprint.changed(w,pos);
   BlockPos top=pos.func_177984_a();var upper=w.func_180495_p(top);check(upper.func_177230_c() instanceof TumblestoneFootprint.Upper||DayCareFootprint.upper(upper),"Actual tall companion created "+id);
   for(Direction d:Direction.values()){
    BlockPos neighbour=top.func_177972_a(d);if(neighbour.equals(pos))continue;w.func_180501_a(neighbour,Blocks.field_150348_b.func_176223_P(),2);
    check(Block.func_176225_a(w.func_180495_p(neighbour),w,neighbour,d.func_176734_d()),"Adjacent stone face remains rendered "+id+" "+d);
   }
   check(!upper.func_196952_d(w,top).func_197766_b(),"Physical upper collision preserved "+id);kinds.add(id);
  }report.add("faceVisibilityBlocks",kinds);
 }
 static void furniture(ServerPlayerEntity player){
  var w=player.func_71121_q();JsonArray rotations=new JsonArray();
  for(Rotation r:Rotation.values()){
   BlockPos home=new BlockPos(80+r.ordinal()*24,90,80);w.func_175726_f(home);AuthorBossFeature.build(w,home,false,"minecraft:stone_bricks",r);
   BlockPos door=AuthorRoomRotation.pos(home,0,1,6,r),porch=AuthorRoomRotation.pos(home,0,0,7,r);check(w.func_175623_d(door)&&w.func_180495_p(porch).func_177230_c()==ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:stone_bricks")),"Door and porch rotated "+r);
   BlockPos chair=AuthorRoomRotation.pos(home,-1,1,-4,r),table=AuthorRoomRotation.pos(home,-1,1,-5,r);
   check(w.func_180495_p(chair).func_177229_b(HorizontalBlock.field_185512_D)==r.func_185831_a(Direction.NORTH),"Chair faces worktable "+r);
   check(w.func_180495_p(table.func_177984_a()).func_177229_b(HorizontalBlock.field_185512_D)==r.func_185831_a(Direction.SOUTH),"Worktable faces chair "+r);
   var tile=(com.pixelmonmod.pixelmon.blocks.tileentity.EndTableTileEntity)w.func_175625_s(table);check(tile!=null&&!tile.func_191420_l(),"Real rotated table reward inventory exists "+r);var saved=tile.func_189515_b(new CompoundNBT());AuthorRoomFurnishings.place(w,home,r);check(tile.func_189515_b(new CompoundNBT()).equals(saved),"Rotated repeated placement preserves prize data "+r);
   var npc=AuthorBossTeams.create(w,home,"forest");npc.getPersistentData().func_74768_a(AuthorRoomRotation.TAG,r.ordinal());AuthorBossArenas.active(npc,player);check(w.func_180495_p(door).func_177230_c()==Blocks.field_150411_aY,"Actual challenge closes rotated doorway "+r);AuthorBossArenas.open(npc);check(w.func_175623_d(door),"Actual challenge reopens rotated doorway "+r);npc.func_70106_y();rotations.add(r.name());
  }
  report.add("rotations",rotations);
 }
 static void smelt(ServerWorld w){
  ItemStack flesh=item("biomesoplenty:flesh");check(!flesh.func_190926_b(),"Actual BOP flesh exists");var inv=new net.minecraft.inventory.Inventory(flesh);var manager=w.func_199532_z();
  check(manager.func_215371_a(IRecipeType.field_222150_b,inv,w).isPresent(),"Native smelting recipe matches flesh");check(manager.func_215371_a(IRecipeType.field_222151_c,inv,w).isEmpty(),"Native blasting lookup rejects flesh");check(manager.func_215371_a(IRecipeType.field_222152_d,inv,w).isEmpty(),"Native smoking lookup rejects flesh");
  int n=0;for(String id:List.of("minecraft:furnace","minecraft:blast_furnace","minecraft:smoker")){
   BlockPos pos=new BlockPos(10+n++*3,90,30);w.func_180501_a(pos,block(id),3);var tile=(AbstractFurnaceTileEntity)w.func_175625_s(pos);tile.func_70299_a(0,flesh.func_77946_l());tile.func_70299_a(1,item("minecraft:coal"));for(int tick=0;tick<210;tick++)tile.func_73660_a();
   if(id.equals("minecraft:furnace"))check(tile.func_70301_a(2).func_77973_b()==Items.field_151078_bh&&tile.func_70301_a(0).func_190926_b(),"Actual furnace ticks consume flesh and produce rotten flesh");else check(tile.func_70301_a(2).func_190926_b()&&tile.func_70301_a(0).func_190916_E()==1&&tile.func_70301_a(1).func_190916_E()==1,"Actual high furnace/smoker cannot cook flesh or consume fuel "+id);
  }
 }
 static void authorFeature(ServerWorld w){
  List<BlockPos> candidates=new ArrayList<>();
  for(int x=2048;x<2088;x++)for(int z=2048;z<2088;z++)if(AuthorBossPolicy.candidate(w.func_72905_C(),x,z,false,"plains"))candidates.add(new BlockPos(x*16+8,80,z*16+8));
  check(candidates.size()==2,"Two nearby real feature candidates available");int n=0;
  for(BlockPos home:candidates){
   w.func_175726_f(home);for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)w.func_180501_a(home.func_177982_a(x,0,z),Blocks.field_150348_b.func_176223_P(),2);
   boolean generated=AuthorBossFeature.INSTANCE.get().func_241855_a(w,w.func_72863_F().func_201711_g(),new Random(27),home,NoFeatureConfig.field_236559_b_);
   check(generated==(n++==0),"Actual author feature retains first candidate and blocks nearby second");
  }
  BlockPos home=candidates.get(0);check(AuthorRoomIndex.get(w).positions(false).contains(home),"Actual accepted author room indexed persistently");
  var bosses=w.func_217357_a(com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer.class,new AxisAlignedBB(home).func_186662_g(8));var npc=bosses.stream().filter(AuthorBossTeams::is).findFirst().orElseThrow();
  check(npc.getPersistentData().func_74764_b(AuthorRoomRotation.TAG),"Actual generated NPC persists rotation");var r=AuthorRoomRotation.get(npc);check(w.func_175623_d(AuthorRoomRotation.pos(home,0,1,6,r)),"Actual generated doorway matches NPC rotation");
  report.addProperty("generatedAuthorRotation",r.name());
  // A real saved author index entry is checked after closing/reopening this private world.
  AuthorRoomIndex.get(w).record(new BlockPos(400000,80,400000),false);
 }
 public static void reload(ServerPlayerEntity player)throws Exception{
  var w=player.func_71121_q();var old=new BlockPos(400000,80,400000);check(AuthorRoomIndex.get(w).positions(false).contains(old),"Existing unloaded author index survives actual game restart");
  check(!AltarSpacing.author(w,old.func_177982_a(2999,0,0)),"Restart imports existing author positions before new generation");
  Files.writeString(Path.of("final190-reload-results.json"),"{\"pass\":true,\"assertions\":"+checks+"}");BossBalance190Probe.log("FINAL190_RELOAD_PASS "+checks);
 }
 public static void run(ServerPlayerEntity player)throws Exception{
  var w=player.func_71121_q();index();structures(w);faces(w);furniture(player);smelt(w);authorFeature(w);report.addProperty("assertions",checks);report.addProperty("pass",true);Files.writeString(Path.of("final190-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));BossBalance190Probe.log("FINAL190_PASS "+checks);
 }
}
