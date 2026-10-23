package qa;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import local.pixelmon.backport.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.*;
import net.minecraft.util.*;
import org.lwjgl.openal.AL10;
public final class PokemonMusicCases {
 static SoundEngine engine;static ISound center,disc,battle,vanilla;static float old;
 static Object field(Object o,String name)throws Exception{for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException e){}throw new NoSuchFieldException(name);}
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static SimpleSound sound(String id,SoundCategory category){return new SimpleSound(new ResourceLocation(id),category,1,1,true,0,ISound.AttenuationType.NONE,0,0,0,false);}
 public static void begin()throws Exception{
  check(!net.minecraft.client.resources.I18n.func_135052_a("pixelmon.backport.author.buff_rate").contains("Format error"),"Author buff sidebar percent renders");
  check(!net.minecraft.client.resources.I18n.func_135052_a("pixelmon.backport.author.protect_rate","80").contains("Format error"),"Author protect sidebar percent renders");
  var mc=Minecraft.func_71410_x();var manager=mc.func_147118_V();engine=(SoundEngine)field(manager,"field_147694_f");old=BattleMusicVolume.get();
  mc.field_71474_y.func_186712_a(SoundCategory.MUSIC,0);mc.field_71474_y.func_186712_a(SoundCategory.RECORDS,0);BattleMusicVolume.set(0);
  center=sound("pixelmon:pixelmon.music.pokecenter",SoundCategory.MUSIC);disc=sound("pixelmon_backport:music.battle.wild_two",SoundCategory.RECORDS);battle=sound("pixelmon:pixelmon.music.battle.wild",SoundCategory.MUSIC);vanilla=sound("minecraft:block.note_block.harp",SoundCategory.BLOCKS);
  for(var song:List.of(center,disc,battle)){check(PokemonMusic.isMusic(song),"Classifies song "+song.func_147650_b());check(song.func_184365_d()==SoundCategory.MASTER,"Independent category "+song.func_147650_b());manager.func_147682_a(song);}
  check(!PokemonMusic.isMusic(vanilla)&&vanilla.func_184365_d()==SoundCategory.BLOCKS,"Vanilla effect untouched");manager.func_147682_a(vanilla);
 }
 @SuppressWarnings("unchecked") static void gains(float factor)throws Exception{
  Map<ISound,ChannelManager.Entry> channels=(Map<ISound,ChannelManager.Entry>)field(engine,"field_217942_m");
  for(ISound song:List.of(center,disc,battle)){
   var channel=channels.get(song);check(channel!=null,"Retains stream "+song.func_147650_b());var done=new CompletableFuture<Float>();
   channel.func_217888_a(source->{try{int id=(Integer)field(source,"field_216441_b");float actual=AL10.alGetSourcef(id,AL10.AL_GAIN);float raw=(Float)field(source,"backport$rawGain");check(Math.abs(actual-raw*factor)<.003,"Actual OpenAL gain "+song.func_147650_b()+" "+actual+" raw="+raw+" expected="+factor);done.complete(actual);}catch(Throwable e){done.completeExceptionally(e);}});
   done.get(4,TimeUnit.SECONDS);
  }
 }
 public static void tick(int tick)throws Exception{
  if(tick==25){gains(0);BattleMusicVolume.set(.5f);}
  if(tick==50){gains(.5f);BattleMusicVolume.set(1);}
  if(tick==75){gains(1);BattleMusicVolume.set(0);}
  if(tick==100){gains(0);BattleMusicVolume.set(old);for(var song:List.of(center,disc,battle,vanilla))Minecraft.func_71410_x().func_147118_V().func_147683_b(song);System.out.println("POKEMON_MUSIC_PASS real native streams under vanilla MUSIC/RECORDS zero; live OpenAL gain0/50/100/0, no stream loss; non-music classification unchanged");}
 }
}
