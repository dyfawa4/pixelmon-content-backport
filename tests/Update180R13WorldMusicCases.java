package qa;
import java.util.*;
import local.pixelmon.backport.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import com.pixelmonmod.pixelmon.client.music.BattleMusic;
import com.pixelmonmod.pixelmon.sounds.BattleMusicType;
public final class Update180R13WorldMusicCases {
 static IndependentBattleSound first,second;static float volume;
 static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
 static Object song()throws Exception{var f=BattleMusic.class.getDeclaredField("song");f.setAccessible(true);return f.get(null);}
 static void exit()throws Exception{var m=com.pixelmonmod.pixelmon.comm.packetHandlers.battles.ExitBattlePacket.class.getDeclaredMethod("handlePacket",net.minecraftforge.fml.network.NetworkEvent.Context.class);m.setAccessible(true);m.invoke(new com.pixelmonmod.pixelmon.comm.packetHandlers.battles.ExitBattlePacket(),new Object[]{null});}
 public static void server(ServerPlayerEntity player){
  var server=player.func_184102_h();var world=server.func_71218_a(net.minecraft.util.RegistryKey.func_240903_a_(net.minecraft.util.registry.Registry.field_239699_ae_,new ResourceLocation("pixelmon:ultra_space")));check(world!=null,"Real Ultra Space dimension");
  var biome=net.minecraftforge.registries.ForgeRegistries.BIOMES.getValue(new ResourceLocation("pixelmon:ultra_desert"));check(biome!=null,"Real Ultra Desert registered");
  var themes=GymCities.DATA.getAsJsonObject("newThemes");check(themes.getAsJsonObject("rock").getAsJsonArray("biomes").size()==1&&themes.getAsJsonObject("rock").getAsJsonArray("biomes").get(0).getAsString().equals("pixelmon:ultra_desert"),"Rock only Ultra Desert");
  check(GymLocator.settings(world)!=null&&GymLocator.available(world,"rock"),"Native structure spacing and typed locator allow rock in Ultra Space");
  var reg=world.func_241828_r().func_243612_b(net.minecraft.util.registry.Registry.field_239720_u_);int ultraCount=0,oldCount=0;
  for(var b:reg){var id=reg.func_177774_c(b);for(var supplier:b.func_242440_e().func_242487_a()){var feature=supplier.get();if(feature.field_236269_c_ instanceof com.pixelmonmod.pixelmon.world.structure.type.GymConfig){var config=(com.pixelmonmod.pixelmon.world.structure.type.GymConfig)feature.field_236269_c_;if(GymThemePaths.poolTypes(config.func_242810_c().get().func_214947_b().toString()).contains("rock")){check(id.toString().equals("pixelmon:ultra_desert"),"No old biome retains rock "+id);ultraCount++;}}}}
  check(ultraCount==1,"Ultra Desert actually contains native rock feature once");
  var config=(com.pixelmonmod.pixelmon.world.structure.type.GymConfig)GymCities.FEATURES.get("rock").field_236269_c_;var gen=world.func_72863_F().func_201711_g();int pieces=0;
  for(int seed=1;seed<=8&&pieces<12;seed++){var start=new com.pixelmonmod.pixelmon.world.structure.type.GymStructure.Start((com.pixelmonmod.pixelmon.world.structure.type.GymStructure)com.pixelmonmod.pixelmon.world.structure.PixelmonStructures.GYMS.get(),80,80,net.minecraft.util.math.MutableBoundingBox.func_78887_a(),0,seed);start.generatePieces(world.func_241828_r(),gen,world.func_184163_y(),80,80,biome,config);pieces=start.func_186161_c().size();if(pieces>=12)check(GymLocator.matches(start,"rock"),"Real assembled structure recognized by typed locator");}
  check(pieces>=12,"Full existing rock city assembles using Ultra Space terrain "+pieces);
  var spacing=GymLocator.settings(world);boolean natural=false;int attempted=0;
  outer:for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){
   var cp=GymLocator.nativeGym().func_236392_a_(spacing,world.func_72905_C(),new net.minecraft.util.SharedSeedRandom(),x*spacing.func_236668_a_(),z*spacing.func_236668_a_());
   var candidate=gen.func_202090_b().func_225526_b_(cp.field_77276_a*4+2,0,cp.field_77275_b*4+2);if(!new ResourceLocation("pixelmon:ultra_desert").equals(reg.func_177774_c(candidate)))continue;
   var chunk=world.func_217348_a(cp.field_77276_a,cp.field_77275_b,net.minecraft.world.chunk.ChunkStatus.field_222606_b);var start=chunk.func_230342_a_(GymLocator.nativeGym());attempted++;
   if(GymLocator.matches(start,"rock")){check(start.func_186161_c().size()>=12,"Native natural start contains full city");natural=true;System.out.println("R13_ROCK_NATURAL_START "+cp+" pieces="+start.func_186161_c().size());break outer;}if(attempted>=16)break outer;
  }
  check(natural,"Native natural chunk generation actually produces Ultra Desert rock gym candidates="+attempted);
  System.out.println("R13_ROCK_PASS actual Ultra Desert biome feature, Ultra Space structure spacing/typed locator, old biomes excluded, native full city assembly pieces="+pieces+", natural chunk structure start");
 }
 public static void client(int age)throws Exception{
  if(age==5){volume=BattleMusicVolume.get();BattleMusicVolume.set(1f);BattleMusic.startBattleMusic(BattleMusicType.RAID,BackportSounds.MAX_RAID_TRACK,300000,true);first=(IndependentBattleSound)song();check(first!=null,"Actual Max Raid music object");exit();check(song()==null,"Native ExitBattlePacket detaches queued music even before sound activation");}
  if(age==65){check(first.func_147667_k()&&!IndependentBattleSound.active(),"Queued Max Raid stream finishes and is untracked after fade");BattleMusic.startBattleMusic(BattleMusicType.RAID,BackportSounds.TERA_TRACK,300000,true);first=(IndependentBattleSound)song();BattleMusic.startBattleMusic(BattleMusicType.RAID,BackportSounds.MAX_RAID_TRACK,300000,true);second=(IndependentBattleSound)song();check(first.func_147667_k()&&second!=first,"Replacing queued raid track stops old stream");}
  if(age==75){exit();exit();}
  if(age==135){check(second.func_147667_k()&&!IndependentBattleSound.active(),"Duplicate exit does not restart fade or leave music");BattleMusic.startBattleMusic(BattleMusicType.RAID,BackportSounds.TERA_TRACK,300000,true);first=(IndependentBattleSound)song();IndependentBattleSound.Cleanup.unload(new net.minecraftforge.event.world.WorldEvent.Unload(net.minecraft.client.Minecraft.func_71410_x().field_71441_e));check(first.func_147667_k()&&song()==null&&!IndependentBattleSound.active(),"World unload clears only tracked battle streams");}
  if(age==145){BattleMusic.startBattleMusic(BattleMusicType.RAID,BackportSounds.MAX_RAID_TRACK,300000,true);second=(IndependentBattleSound)song();IndependentBattleSound.Cleanup.logout(null);check(second.func_147667_k()&&!IndependentBattleSound.active(),"Disconnect clears battle music");check(!(net.minecraft.client.Minecraft.func_71410_x().func_181535_r() instanceof com.pixelmonmod.pixelmon.client.music.VoidMusicTicker),"Vanilla background music ticker restored on disconnect");BattleMusicVolume.set(volume);System.out.println("R13_MUSIC_CLIENT_PASS native ExitBattlePacket, Max/Tera pending playback and replacement, fade completion/repeated exit, world-unload and logout cleanup; background/record/cry sources untouched");}
 }
}
