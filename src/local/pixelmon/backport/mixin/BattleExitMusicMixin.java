package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.comm.packetHandlers.battles.ExitBattlePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Raid exits do not necessarily close the ordinary BattleScreen. */
@Mixin(value=ExitBattlePacket.class,remap=false)
public abstract class BattleExitMusicMixin {
 @Inject(method="handlePacket",at=@At("HEAD"))
 private void backport$endMusic(net.minecraftforge.fml.network.NetworkEvent.Context context,CallbackInfo ci){com.pixelmonmod.pixelmon.client.music.BattleMusic.endBattleMusic();}
}
