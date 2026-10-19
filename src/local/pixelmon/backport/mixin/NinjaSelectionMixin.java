package local.pixelmon.backport.mixin;
import local.pixelmon.backport.NinjaTraining;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.comm.packetHandlers.battles.ChooseAttackPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ChooseAttackPacket.class,remap=false)
public abstract class NinjaSelectionMixin {
    @Shadow public java.util.UUID pokemonUUID;
    @Shadow private int buttonId;
    @Shadow private int battleIndex;
    @Shadow public boolean dynamaxing;
    @Inject(method="handlePacket",at=@At("HEAD"),cancellable=true)
    private void backport$forbidBonusSetup(net.minecraftforge.fml.network.NetworkEvent.Context ctx,CallbackInfo ci){
        var player=ctx.getSender();if(player==null)return;var b=BattleRegistry.getBattle(player);if(b==null||b.battleIndex!=battleIndex)return;
        var part=b.getParticipantForEntity(player);if(part==null)return;var p=part.getPokemonFromUUID(pokemonUUID);if(p==null)return;
        if(buttonId>=0&&buttonId<8){
            var attack=p.getMoveset().get(buttonId%4);
            if(NinjaTraining.move(attack)&&p.isDynamax==0&&!dynamaxing&&!NinjaTraining.allowed(p)){b.sendToAll("pixelmon_backport.battle.ninja_blocked",p.getNickname());NinjaTraining.chooseAgain(p);ci.cancel();}
        }
    }
}
