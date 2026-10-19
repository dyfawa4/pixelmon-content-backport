package local.pixelmon.backport.mixin;
import local.pixelmon.backport.TeraRaids;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.comm.packetHandlers.battles.ChooseAttackPacket;
import net.minecraftforge.fml.network.NetworkEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Also normalize stale/forged native selections before the native handler reads them. */
@Mixin(value=ChooseAttackPacket.class,remap=false)
public abstract class TeraRaidChoiceMixin {
    @Shadow private int battleIndex;
    @Shadow private int buttonId;
    @Shadow public boolean megaEvolving;
    @Shadow public boolean dynamaxing;
    @Inject(method="handlePacket",at=@At("HEAD"))
    private void backport$onlyTera(NetworkEvent.Context context,CallbackInfo ci){
        var player=context.getSender();if(player==null)return;
        var battle=BattleRegistry.getBattle(player);
        if(battle==null || battle.battleIndex!=battleIndex || TeraRaids.get(battle)==null)return;
        megaEvolving=false;dynamaxing=false;
        if(buttonId>=4 && buttonId<8)buttonId-=4;
    }
}
