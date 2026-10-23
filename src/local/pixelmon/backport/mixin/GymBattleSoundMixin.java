package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.client.music.BattleMusic;
import com.pixelmonmod.pixelmon.sounds.BattleMusicType;
import net.minecraft.util.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Apply after PixelTweaks' priority-1000 overwrite, with or without that optional mod. */
@Mixin(value=BattleMusic.class,remap=false,priority=1100)
public abstract class GymBattleSoundMixin {
 @Redirect(method="startBattleMusic",at=@At(value="NEW",target="com/pixelmonmod/pixelmon/client/music/BattleMusic$FixedTrackSound"),require=1)
 private static BattleMusic.FixedTrackSound backport$independent(SoundEvent sound,int track,net.minecraft.util.SoundCategory category,float volume,float pitch,boolean loop,int delay,net.minecraft.client.audio.ISound.AttenuationType attenuation,float x,float y,float z){
  return new local.pixelmon.backport.IndependentBattleSound(sound,track,category,volume,pitch,loop,delay,attenuation,x,y,z);
 }
 @Redirect(method="startBattleMusic",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/client/music/PixelmonMusic;fadeSoundToStart(Lnet/minecraft/client/audio/ISound;J)V"),require=1)
 private static void backport$start(net.minecraft.client.audio.ISound sound,long millis){((local.pixelmon.backport.IndependentBattleSound)sound).start(millis);}
 @Redirect(method="endBattleMusic",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/client/music/PixelmonMusic;fadeSoundToStop(Lnet/minecraft/client/audio/ISound;JLjava/lang/Runnable;)V"),require=1)
 private static void backport$stop(net.minecraft.client.audio.ISound sound,long millis,Runnable callback){
  if(sound instanceof local.pixelmon.backport.IndependentBattleSound)((local.pixelmon.backport.IndependentBattleSound)sound).stop(millis,callback);
  else com.pixelmonmod.pixelmon.client.music.PixelmonMusic.fadeSoundToStop(sound,millis,callback);
 }
 @ModifyVariable(method="startBattleMusic",at=@At("STORE"),ordinal=0,require=1)
 private static SoundEvent backport$gymTrack(SoundEvent sound,BattleMusicType type,int track,long length,boolean loop){
  if(type==BattleMusicType.GYM)return local.pixelmon.backport.BackportSounds.GYM_LEADER.get();
  if(type==BattleMusicType.WILD)return track==local.pixelmon.backport.BackportSounds.WILD_TWO_TRACK?local.pixelmon.backport.BackportSounds.WILD_TWO.get():local.pixelmon.backport.BackportSounds.WILD.get();
  if(type==BattleMusicType.RAID&&track==local.pixelmon.backport.BackportSounds.TERA_TRACK)return local.pixelmon.backport.BackportSounds.TERA_RAID.get();
  if(type==BattleMusicType.RAID&&track==local.pixelmon.backport.BackportSounds.MAX_RAID_TRACK)return local.pixelmon.backport.BackportSounds.RAID_DISC.get();
  return sound;
 }
}
