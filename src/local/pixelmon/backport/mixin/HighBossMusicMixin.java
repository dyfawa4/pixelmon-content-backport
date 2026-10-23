package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.sounds.BattleMusicType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=PlayerParticipant.class,remap=false)
public abstract class HighBossMusicMixin {
 @Redirect(method="openGui",at=@At(value="NEW",target="com/pixelmonmod/pixelmon/comm/packetHandlers/battles/music/PlayBattleMusicPacket"),require=1)
 private com.pixelmonmod.pixelmon.comm.packetHandlers.battles.music.PlayBattleMusicPacket backport$teraMusic(BattleMusicType type,int index,long duration,boolean repeat){
  return local.pixelmon.backport.RaidMusic.packet((PlayerParticipant)(Object)this,type,index,duration,repeat);
 }
 @Inject(method="getBattleMusicType",at=@At("RETURN"),cancellable=true,require=1)
 private void backport$legendaryBossMusic(CallbackInfoReturnable<BattleMusicType> cir){
  if(cir.getReturnValue()!=BattleMusicType.BOSS)return;
  var player=(PlayerParticipant)(Object)this;
  for(var opponent:player.getOpponents())if(opponent instanceof WildPixelmonParticipant)for(var pokemon:opponent.controlledPokemon){
   var tier=pokemon.entity==null?null:pokemon.entity.getBossTier();if(tier!=null&&java.util.Set.of("epic","legendary","ultimate","drowned").contains(tier.getID())){cir.setReturnValue(BattleMusicType.LEGENDARY);return;}
  }
 }
}
