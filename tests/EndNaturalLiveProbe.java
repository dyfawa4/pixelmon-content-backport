import java.nio.file.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.client.ServerStorageDisplay;
import com.pixelmonmod.pixelmon.entities.npcs.registry.DropItemRegistry;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import com.pixelmonmod.pixelmon.client.gui.starter.ChooseStarterScreen;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.structure.*;
import net.minecraft.world.chunk.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
@Mod("backport_end_probe") @Mod.EventBusSubscriber(modid="backport_end_probe")
public class EndNaturalLiveProbe {
 static final String WORLD="backport-end-full-city-v7";static volatile boolean done;static int scanned;static GymLocator.Search search;static net.minecraft.command.CommandSource source;static int stage;static net.minecraft.world.server.ServerWorld fullWorld;static java.util.List<ChunkPos> fullChunks=new java.util.ArrayList<>();static java.util.List<java.util.concurrent.CompletableFuture<?>> fullRequests=new java.util.ArrayList<>();static net.minecraft.util.math.MutableBoundingBox cityBox;static int fullCursor,fullTicks;static java.util.List<StructurePiece> cityPieces;static int screenshotTicks;static final net.minecraft.world.server.TicketType<ChunkPos> FULL_TICKET=net.minecraft.world.server.TicketType.func_219484_a("private_end_city",java.util.Comparator.comparingLong(ChunkPos::func_201841_a));static Object compassWorker;static final java.util.List<String> messages=new java.util.ArrayList<>();static java.util.List<ChunkPos> candidates=new java.util.ArrayList<>();
 static boolean enabled(){
  if(!WORLD.equals(System.getProperty("backport.manual.world")))return false;
  var server=ServerLifecycleHooks.getCurrentServer();
  return server==null||server.func_240776_a_(net.minecraft.world.storage.FolderName.field_237253_i_).normalize().getFileName().toString().equals(WORLD);
 }
 static void log(String s){System.out.println("END_NATURAL "+s);try{Files.writeString(Path.of("end-natural-result.txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}}
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
  if(!enabled()||done||e.phase!=TickEvent.Phase.END)return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||server.func_184103_al().func_181057_v().isEmpty())return;
  try{var world=server.func_71218_a(World.field_234920_i_);var gen=world.func_72863_F().func_201711_g();
   if(candidates.isEmpty()){
    var s=GymLocator.settings(world);log("features="+server.func_240793_aU_().func_230418_z_().func_236222_c_()+" settings="+s+" available="+GymLocator.available(world,"psychic")+" seed="+world.func_72905_C());
    for(var b:gen.func_202090_b().func_235203_c_())for(var f:b.func_242440_e().func_242487_a()){var cf=f.get();if(cf.field_236268_b_==GymLocator.nativeGym())log("biome="+b.getRegistryName()+" pool="+cf.field_236269_c_);}
    if(s==null){done=true;return;}log("spacing="+s.func_236668_a_());
    for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)candidates.add(GymLocator.nativeGym().func_236392_a_(s,world.func_72905_C(),new SharedSeedRandom(),x*s.func_236668_a_(),z*s.func_236668_a_()));
    candidates.sort(java.util.Comparator.comparingLong(p->(long)p.field_77276_a*p.field_77276_a+(long)p.field_77275_b*p.field_77275_b));
   }
   if(stage==0){
    var player=server.func_184103_al().func_181057_v().get(0);
    var party=StorageProxy.getParty(player);party.starterPicked=true;party.guiOpened=true;
    for(String[] pair:new String[][]{{"pink_nectar","minecraft:pink_tulip"},{"red_nectar","minecraft:red_tulip"},{"purple_nectar","pixelmon:ultra_desert_cactus"},{"yellow_nectar","minecraft:dandelion"}}){
     var recipe=(net.minecraft.item.crafting.ShapedRecipe)server.func_199529_aN().func_215367_a(new ResourceLocation("pixelmon_backport",pair[0])).orElseThrow();
     var container=new net.minecraft.inventory.container.Container(null,-1){public boolean func_75145_c(net.minecraft.entity.player.PlayerEntity p){return true;}};
     var grid=new net.minecraft.inventory.CraftingInventory(container,3,3);
     var honey=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft:honey_bottle"));
     var flower=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(pair[1]));
     for(int slot:new int[]{1,3,5,7})grid.func_70299_a(slot,new net.minecraft.item.ItemStack(honey));grid.func_70299_a(4,new net.minecraft.item.ItemStack(flower));
     if(!recipe.func_77569_a(grid,world))throw new AssertionError("Recipe grid "+pair[0]);
     if(recipe.func_77572_b(grid).func_190916_E()!=1)throw new AssertionError("Recipe output count");
     var remain=recipe.func_179532_b(grid);for(int slot:new int[]{1,3,5,7})if(!remain.get(slot).func_77973_b().getRegistryName().toString().equals("minecraft:glass_bottle"))throw new AssertionError("Honey bottle remainder");
     grid.func_70299_a(0,new net.minecraft.item.ItemStack(honey));if(recipe.func_77569_a(grid,world))throw new AssertionError("Extra corner accepted");grid.func_70299_a(0,net.minecraft.item.ItemStack.field_190927_a);grid.func_70299_a(7,net.minecraft.item.ItemStack.field_190927_a);if(recipe.func_77569_a(grid,world))throw new AssertionError("Missing honey accepted");
     if(DropItemRegistry.tier3.stream().anyMatch(stack->stack.func_77973_b()==recipe.func_77571_b().func_77973_b()))throw new AssertionError("Nectar remains in master loot");
    }log("NECTAR_NATIVE_PASS four recipes, exact grid, four glass remainders, loot removal");
    source=new net.minecraft.command.CommandSource(new net.minecraft.command.ICommandSource(){public void func_145747_a(net.minecraft.util.text.ITextComponent message,java.util.UUID id){messages.add(message.getString());log("COMMAND "+message.getString());}public boolean func_195039_a(){return true;}public boolean func_195040_b(){return true;}public boolean func_195041_r_(){return false;}},new net.minecraft.util.math.vector.Vector3d(0,70,0),net.minecraft.util.math.vector.Vector2f.field_189974_a,world,4,"EndProbe",new net.minecraft.util.text.StringTextComponent("EndProbe"),server,null);
    server.func_195571_aL().func_197054_a().execute("locategym psychic 8192",source);stage=1;return;
   }
   if(stage==1){server.func_195571_aL().func_197054_a().execute("locategym cancel",source);var jobs=GymLocator.class.getDeclaredField("jobs");jobs.setAccessible(true);if(!((java.util.Map<?,?>)jobs.get(null)).isEmpty())throw new AssertionError("Cancel left gym jobs");log("CANCEL_NATIVE_PASS");stage=2;}
   if(stage==3){
    if(messages.stream().anyMatch(m->m.contains("backport.gym.locate.found")||m.contains("找到超能力属性道馆"))){log("LOCATE_COMMAND_PASS");stage=4;}else if(++scanned>200)throw new AssertionError("Real /locate did not return found");return;
   }
   if(stage==4){
    if(fullCursor<fullChunks.size()){
     var p=fullChunks.get(fullCursor++);world.func_72863_F().func_217228_a(FULL_TICKET,p,2,p);
     fullRequests.add(((local.pixelmon.backport.mixin.CompassChunkRequestInvoker)world.func_72863_F()).backport$requestChunk(p.field_77276_a,p.field_77275_b,ChunkStatus.field_222617_m,true));return;
    }
    if(fullRequests.stream().anyMatch(f->!f.isDone())){if(++fullTicks>800)throw new AssertionError("Full city chunk generation timed out");return;}
    for(var f:fullRequests)f.join();
    var trainers=world.func_175647_a(NPCTrainer.class,new net.minecraft.util.math.AxisAlignedBB(cityBox.field_78897_a,0,cityBox.field_78896_c,cityBox.field_78893_d+1,256,cityBox.field_78892_f+1),npc->GymCities.isLeader(npc));
    if(trainers.isEmpty())throw new AssertionError("Naturally placed city lacks its gym leader");
    verifyHouses(world);
    log("CITY_PLACEMENT_PASS full chunks="+fullChunks.size()+" gym leaders="+trainers.size()+" bbox="+cityBox);
    for(var p:fullChunks)world.func_72863_F().func_217222_b(FULL_TICKET,p,2,p);
    var player=server.func_184103_al().func_181057_v().get(0);player.func_200619_a(world,cityBox.field_78897_a+256,110,cityBox.field_78896_c+256,0,0);
    var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("explorerscompass:explorerscompass"));if(item==null)throw new AssertionError("Compass mod missing from integration probe");
    var stack=new net.minecraft.item.ItemStack(item);stack.func_196082_o().func_74778_a("StructureKey","pixelmon_backport:psychic_gym");
    var workerClass=Class.forName("com.chaosthedude.explorerscompass.util.StructureSearchWorker");
    compassWorker=workerClass.getConstructors()[0].newInstance(world,player,stack,net.minecraftforge.registries.ForgeRegistries.STRUCTURE_FEATURES.getValue(new ResourceLocation("pixelmon_backport:psychic_gym")),player.func_233580_cy_());stage=5;fullTicks=0;return;
   }
   if(stage==5){
    var c=compassWorker.getClass();c.getMethod("doWork").invoke(compassWorker);
    if((boolean)c.getField("finished").get(compassWorker)){
     var stack=(net.minecraft.item.ItemStack)c.getField("stack").get(compassWorker);var tag=stack.func_77978_p();
     if(tag==null||!tag.func_74764_b("FoundX")||!tag.func_74764_b("FoundZ"))throw new AssertionError("Compass did not store a found city");
     log("COMPASS_NATIVE_PASS x="+tag.func_74762_e("FoundX")+" z="+tag.func_74762_e("FoundZ"));
     var player=server.func_184103_al().func_181057_v().get(0);server.func_195571_aL().func_197054_a().execute("gamemode spectator BackportVisualTest",source);player.func_200619_a(world,(cityBox.field_78897_a+cityBox.field_78893_d)/2.0,210,(cityBox.field_78896_c+cityBox.field_78892_f)/2.0,0,90);stage=6;return;
    }if(++fullTicks>600)throw new AssertionError("Compass integration timeout");return;
   }
   if(stage==6)return;
   if(search==null)search=new GymLocator.Search(world,new BlockPos(10704,70,7408),"psychic",8192);
   var found=search.step();scanned++;
   if(found!=null){log("NATURAL_PASS "+found);search.close();
    var cp=new ChunkPos(found);var root=world.func_217348_a(cp.field_77276_a,cp.field_77275_b,ChunkStatus.field_222606_b);var start=root.func_230342_a_(GymLocator.nativeGym());
    if(!EndGymCities.complete(gen,start.func_186161_c()))throw new AssertionError("Whole city support invalid");cityBox=start.func_75071_a();cityPieces=new java.util.ArrayList<>(start.func_186161_c());
    log("FULL_DISTRICT pieces="+start.func_186161_c().size()+" houses="+EndGymCities.houses(start.func_186161_c())+" roads="+EndGymCities.roads(start.func_186161_c()));
    if(EndGymCities.houses(start.func_186161_c())<8||EndGymCities.roads(start.func_186161_c())<8)throw new AssertionError("Full city district too small");
    for(int xx=cityBox.field_78897_a>>4;xx<=cityBox.field_78893_d>>4;xx++)for(int zz=cityBox.field_78896_c>>4;zz<=cityBox.field_78892_f>>4;zz++)fullChunks.add(new ChunkPos(xx,zz));
    source=source.func_201009_a(new net.minecraft.util.math.vector.Vector3d(found.func_177958_n(),70,found.func_177952_p()));server.func_195571_aL().func_197054_a().execute("locate pixelmon_backport:psychic_gym",source);stage=3;scanned=0;return;}
   if(scanned%100==0)log("ticks="+scanned);
   if(search.finished()){log("NATURAL_NONE all candidates within 8192");search.close();done=true;}

  }catch(Throwable ex){ex.printStackTrace();log("FAIL "+ex);done=true;}
 }
 static void verifyHouses(net.minecraft.world.server.ServerWorld world){
  int houses=0,total=0;
  for(var raw:cityPieces){if(!raw.toString().contains("/gym_psychic_house_"))continue;
   var piece=(AbstractVillagePiece)raw;
   var m=java.util.regex.Pattern.compile("pixelmon_backport:gyms/psychic/gym_psychic_[a-z_]+").matcher(piece.toString());if(!m.find())throw new AssertionError("Unknown house template");String id=m.group();
   var nbt=world.func_184163_y().func_200219_b(new ResourceLocation(id)).func_189552_a(new net.minecraft.nbt.CompoundNBT());
   var palette=nbt.func_150295_c("palette",10);var blocks=nbt.func_150295_c("blocks",10);int tested=0,matched=0;
   for(int i=0;i<blocks.size();i++){
    var block=blocks.func_150305_b(i);var state=palette.func_150305_b(block.func_74762_e("state"));String name=state.func_74779_i("Name");
    if(!(name.contains("purpur")||name.endsWith("_terracotta")||name.endsWith("_planks")||name.endsWith("_wool")||name.endsWith("_bricks")||name.contains("quartz")||name.contains("stained_glass")))continue;
    var list=block.func_150295_c("pos",3);var local=new BlockPos(list.func_186858_c(0),list.func_186858_c(1),list.func_186858_c(2));
    var rotated=net.minecraft.world.gen.feature.template.Template.func_207669_a(local,net.minecraft.util.Mirror.NONE,piece.func_214809_Y_(),BlockPos.field_177992_a);
    var pos=piece.func_214828_c().func_177982_a(rotated.func_177958_n(),rotated.func_177956_o(),rotated.func_177952_p());
    tested++;if(world.func_180495_p(pos).func_177230_c()==net.minecraft.nbt.NBTUtil.func_190008_d(state).func_177230_c())matched++;
   }
   if(tested<10||matched<tested*.98)throw new AssertionError("House not fully materialized "+id+" matched="+matched+"/"+tested);houses++;total+=tested;
  }
  if(houses<8)throw new AssertionError("Too few materialized houses");log("HOUSE_MATERIALIZATION_PASS houses="+houses+" structural blocks="+total);
 }
 @Mod.EventBusSubscriber(modid="backport_end_probe",value=net.minecraftforge.api.distmarker.Dist.CLIENT) public static class Client {
 static boolean opened,hidden;static int close;
 @SubscribeEvent public static void gui(net.minecraftforge.client.event.GuiOpenEvent e){if(enabled()&&e.getGui() instanceof ChooseStarterScreen){ServerStorageDisplay.showStarterScreen=false;e.setCanceled(true);}}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(!enabled()||e.phase!=TickEvent.Phase.END)return;var mc=net.minecraft.client.Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;
 if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}
 if(!opened&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_(WORLD);}if(mc.field_71439_g!=null){ServerStorageDisplay.showStarterScreen=false;if(mc.field_71462_r instanceof ChooseStarterScreen)mc.func_147108_a(null);}if(stage==6){mc.field_71474_y.field_151451_c=6;mc.field_71474_y.field_74333_Y=10;
 if(++screenshotTicks==180){var window=mc.func_228018_at_();net.minecraft.util.ScreenShotHelper.func_148259_a(new java.io.File("."),"full-city-overview.png",window.func_198107_o(),window.func_198087_p(),mc.func_147110_a(),message->log("SCREENSHOT "+message.getString()));done=true;}}
 if(done&&++close>20)mc.func_71400_g();}
 }
}
