import java.nio.file.*;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.client.ServerStorageDisplay;
import com.pixelmonmod.pixelmon.client.gui.starter.ChooseStarterScreen;
import com.pixelmonmod.pixelmon.client.gui.battles.BattleScreen;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.links.DelegateLink;
import com.pixelmonmod.pixelmon.api.registries.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.entities.pokeballs.*;
import com.pixelmonmod.pixelmon.items.medicine.MedicineItem;
import com.pixelmonmod.pixelmon.world.structure.PixelmonStructures;
import com.pixelmonmod.pixelmon.world.structure.type.*;
import local.pixelmon.backport.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.server.ServerWorld;

/** Opt-in disposable client probe. Never included in the core or tester map helper. */
@Mod("backport_update_probe")
@Mod.EventBusSubscriber(modid="backport_update_probe")
public final class Update170LiveProbe {
 static final String WORLD="backport-update-20261021";static volatile int phase;static int ticks,attempts,accepted,scanned;static Pokemon hooh;static final List<String> failures=new ArrayList<>();
 static boolean enabled(){
  if(!WORLD.equals(System.getProperty("backport.manual.world")))return false;
  var server=ServerLifecycleHooks.getCurrentServer();
  return server==null||server.func_240776_a_(net.minecraft.world.storage.FolderName.field_237253_i_).normalize().getFileName().toString().equals(WORLD);
 }
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 static void result(String message){try{Files.writeString(Path.of("update-probe-result.txt"),message);}catch(Exception e){throw new RuntimeException(e);}System.out.println("UPDATE_PROBE_"+message);phase=99;}
 static Pokemon mon(int dex){Pokemon p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(100);p.initialize();p.heal();return p;}
 static void vegetation(ServerWorld world,GymStructure.Start start,int rootX,int rootZ){
  MutableBoundingBox box=start.func_75071_a();
  var plant=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:chorus_plant"));
  var flower=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:chorus_flower"));
  var stone=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:end_stone"));
  BlockPos inside=new BlockPos(box.field_78897_a+1,80,box.field_78896_c+1),bloom=inside.func_177982_a(1,0,0),outside=inside.func_177982_a(-3,0,0),building=inside.func_177982_a(0,0,1);
  for(BlockPos pos:List.of(inside,bloom,outside,building)){
   world.func_212866_a_(pos.func_177958_n()>>4,pos.func_177952_p()>>4);
   world.func_180501_a(pos.func_177977_b(),stone.func_176223_P(),18);
   world.func_180501_a(pos,(pos.equals(bloom)?flower:pos.equals(building)?stone:plant).func_176223_P(),18);
  }
  Set<ChunkPos> chunks=new HashSet<>();for(BlockPos pos:List.of(inside,bloom,outside,building))chunks.add(new ChunkPos(pos));
  var root=world.func_212866_a_(rootX,rootZ);var previous=root.func_230342_a_(PixelmonStructures.GYMS.get());root.func_230344_a_(PixelmonStructures.GYMS.get(),start);
  EndGymVegetation cleaner=new EndGymVegetation();
  try{
   cleaner.loaded(new net.minecraftforge.event.world.ChunkEvent.Load(root));
   for(var pos:chunks)cleaner.loaded(new net.minecraftforge.event.world.ChunkEvent.Load(world.func_212866_a_(pos.field_77276_a,pos.field_77275_b)));
   for(int i=0;i<64;i++)cleaner.tick(new TickEvent.WorldTickEvent(net.minecraftforge.fml.LogicalSide.SERVER,TickEvent.Phase.END,world));
  }finally{
   if(previous==null)root.func_201609_c().remove(PixelmonStructures.GYMS.get());else root.func_230344_a_(PixelmonStructures.GYMS.get(),previous);
   cleaner.unloaded(new net.minecraftforge.event.world.WorldEvent.Unload(world));
  }
  check(world.func_175623_d(inside)&&world.func_175623_d(bloom),"Actual chunk-load/tick cleanup did not remove city chorus plants and flowers");
  check(world.func_180495_p(outside).func_177230_c()==plant&&world.func_180495_p(building).func_177230_c()==stone,"Chorus cleanup damaged surroundings/buildings");
  check(world.func_72863_F().func_225313_a(100000,100000)==null&&EndGymVegetation.clean(world,new ChunkPos(100000,100000),List.of(box))==0&&world.func_72863_F().func_225313_a(100000,100000)==null,"Chorus cleanup forced an absent chunk to load");
  System.out.println("UPDATE_PROBE_CHORUS_CLEAN_PASS");
 }
 @SubscribeEvent public static void server(TickEvent.ServerTickEvent event){
  if(!enabled()||event.phase!=TickEvent.Phase.END||phase>=99)return;
  var server=ServerLifecycleHooks.getCurrentServer();if(server==null||server.func_184103_al().func_181057_v().isEmpty())return;
  ServerPlayerEntity player=server.func_184103_al().func_181057_v().get(0);
  try{
   if(phase==0){
    for(var item:List.of(PixelmonItems.revive,PixelmonItems.max_revive,PixelmonItems.revival_herb)){
     Pokemon p=mon(143);p.setHealth(0);check(((MedicineItem)item).useMedicine(new DelegateLink(p),-1),"Actual registered revival rejected");
     check(p.getHealth()==(item==PixelmonItems.revive?p.getMaxHealth()/2:p.getMaxHealth()),"Actual revival amount "+item.getRegistryName());
    }
    for(String name:CapBallPreference.BALLS){
     Pokemon p=mon(CapSpecies.DEX);p.getPersistentData().func_74778_a(CapBallPreference.TAG,name);var ball=PokeBallRegistry.getPokeBall(name).getValueUnsafe();
     check(ball.getCaptureMethod().calculateBallBonus(ball,null,player,p,PokeBallMode.BATTLE)==2,"Native battle bonus "+name);
     check(ball.getCaptureMethod().calculateBallBonus(ball,null,player,p,PokeBallMode.EMPTY)==2,"Native field bonus "+name);
     check(ball.getCaptureMethod().calculateCatchRate(ball,null,player,p,PokeBallMode.EMPTY)==p.getForm().getCatchRate(),"Native additive rate bypass "+name);
     Pokemon loaded=PokemonFactory.create(p.writeToNBT(new CompoundNBT()));check(CapBallPreference.selected(loaded).equals(name),"Real NBT preference "+name);
     check(CapBallPreference.forbidden(PokeBallRegistry.MASTER_BALL.getValueUnsafe(),loaded)&&PokeBallRegistry.MASTER_BALL.getValueUnsafe().getCaptureMethod().calculateBallBonus(PokeBallRegistry.MASTER_BALL.getValueUnsafe(),null,player,loaded,PokeBallMode.BATTLE)==0,"Native Master Ball prohibited");
    }
    Pokemon cap=mon(CapSpecies.DEX);cap.getPersistentData().func_74778_a(CapBallPreference.TAG,"poke_ball");
    var entity=new PixelmonEntity(player.func_71121_q(),cap);entity.func_70107_b(4,65,4);player.func_71121_q().func_217376_c(entity);
    var projectile=new EmptyPokeBallEntity(player.func_71121_q(),player,PokeBallRegistry.POKE_BALL.getValueUnsafe(),false);
    var field=PokeBallEntity.class.getDeclaredField("pixelmon");field.setAccessible(true);field.set(projectile,entity);
    var store=EmptyPokeBallEntity.class.getDeclaredMethod("storeCapture");store.setAccessible(true);store.invoke(projectile);
    check(AcquisitionAdvancements.ownedStored(player,cap)&&cap.getFriendship()==255,"Native successful storage must end at max friendship");projectile.func_70106_y();
    hooh=mon(250);StorageProxy.getParty(player).set(0,hooh);phase=1;ticks=0;System.out.println("UPDATE_PROBE_CAP_MEDICINE_NBT_PASS");
   }else if(phase==1&&++ticks>10){check(hooh.setPalette("rainbow"),"Native rainbow palette rejected");phase=2;ticks=0;
   }else if(phase==2&&++ticks>10){
    var advancement=server.func_191949_aK().func_192778_a(new ResourceLocation("pixelmon_backport:legends/palette_250_rainbow"));
    check(advancement!=null&&player.func_192039_O().func_192747_a(advancement).func_192105_a(),"Owned rainbow Ho-Oh did not grant achievement after string palette change");
    System.out.println("UPDATE_PROBE_RAINBOW_PASS");
    try{NpcDecisionVerification.run(player);}catch(Throwable error){error.printStackTrace();failures.add("NPC: "+error);}
    phase=3;
   }else if(phase==3){
    var end=server.func_71218_a(World.field_234920_i_);check(end!=null,"Missing actual End dimension");
    var generator=end.func_72863_F().func_201711_g();int x=193554,z=148042;boolean replay=attempts==0&&scanned==0,candidate=replay; // Recorded natural-island regression, assembly seed 28.
    for(int budget=0;budget<32&&scanned<16384&&!candidate;budget++){
     x=128+17*scanned;z=128+13*scanned;scanned++;candidate=true;
     for(int dx:new int[]{-80,0,80})for(int dz:new int[]{-80,0,80})if(candidate&&generator.func_222532_b(x*16+dx,z*16+dz,net.minecraft.world.gen.Heightmap.Type.WORLD_SURFACE_WG)<50)candidate=false;
     if(scanned%256==0)System.out.println("UPDATE_PROBE_END_SEARCH "+scanned+" assembled="+attempts);
    }
    if(!candidate){if(scanned>=16384)result("FAIL no naturally wide End island found in terrain search; "+failures);return;}
    attempts++;
    GymConfig config=(GymConfig)GymCities.FEATURES.get("psychic").field_236269_c_;
    GymStructure.Start start=new GymStructure.Start((GymStructure)PixelmonStructures.GYMS.get(),x,z,MutableBoundingBox.func_78887_a(),0,replay?28:attempts);
    var biome=generator.func_202090_b().func_225526_b_(x*4,15,z*4);start.generatePieces(end.func_241828_r(),generator,end.func_184163_y(),x,z,biome,config);
    if(start.func_75069_d()){accepted++;check(EndGymCities.complete(generator,start.func_186161_c()),"Native accepted city failed full support recheck");System.out.println("UPDATE_PROBE_END_CITY_ACCEPT pieces="+start.func_186161_c().size()+" "+start.func_75071_a());vegetation(end,start,x,z);}
    if(accepted>0||attempts>=128){if(accepted==0)failures.add("No supported End city found in 128 candidate starts");result(failures.isEmpty()?"PASS native medicine, cap 22-ball exact bonus, NBT, capture storage friendship, rainbow achievement, NPC regression, supported End city and native chorus cleanup; attempts="+attempts:"FAIL "+failures);}
   }
  }catch(Throwable error){error.printStackTrace();result("FAIL "+error);}
 }
 @Mod.EventBusSubscriber(modid="backport_update_probe",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
 public static final class Client {
  static boolean opened;static int closing;
  // Eighteen server-side battles are created and ended in one regression tick.
  // Their late GUI packets are not a playable battle and can race native overlays.
  @SubscribeEvent public static void gui(net.minecraftforge.client.event.GuiOpenEvent event){
   if(enabled()&&event.getGui() instanceof BattleScreen)event.setCanceled(true);
  }
  @SubscribeEvent public static void client(TickEvent.ClientTickEvent event){
   if(!enabled()||event.phase!=TickEvent.Phase.END)return;var mc=net.minecraft.client.Minecraft.func_71410_x();
   mc.field_71474_y.field_82881_y=false; // Private automatic probe must run while unfocused.
   if(!opened&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_(WORLD);}
   if(mc.field_71439_g!=null){ServerStorageDisplay.showStarterScreen=false;if(mc.field_71462_r instanceof ChooseStarterScreen)mc.func_147108_a(null);}
   if(phase>=99&&++closing>20)mc.func_71400_g();
  }
 }
}
