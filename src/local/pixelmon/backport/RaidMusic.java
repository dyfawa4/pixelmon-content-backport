package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import com.pixelmonmod.pixelmon.sounds.BattleMusicType;
import com.pixelmonmod.pixelmon.comm.packetHandlers.battles.music.PlayBattleMusicPacket;
/** The native reliable battle-music packet already handles start, repeat, volume and exit. */
public final class RaidMusic {
 private static final java.util.Map<com.pixelmonmod.pixelmon.battles.controller.BattleController,Integer> WILD_TRACKS=new java.util.WeakHashMap<>();
 private RaidMusic(){}
 public static PlayBattleMusicPacket packet(PlayerParticipant player,BattleMusicType type,int index,long duration,boolean repeat){
  if(type==BattleMusicType.RAID)index=TeraRaids.get(player.bc)!=null?BackportSounds.TERA_TRACK:BackportSounds.MAX_RAID_TRACK;
  if(type==BattleMusicType.WILD&&player.bc!=null)index=WILD_TRACKS.computeIfAbsent(player.bc,battle->player.player.func_70681_au().nextBoolean()?BackportSounds.WILD_ONE_TRACK:BackportSounds.WILD_TWO_TRACK);
  return new PlayBattleMusicPacket(type,index,duration,repeat);
 }
}
