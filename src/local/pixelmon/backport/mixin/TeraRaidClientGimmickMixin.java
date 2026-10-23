package local.pixelmon.backport.mixin;
import local.pixelmon.backport.TeraRaidGimmicks;
import com.pixelmonmod.pixelmon.client.gui.battles.ClientBattleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ClientBattleManager.class,remap=false)
public abstract class TeraRaidClientGimmickMixin {
    @Inject(method="canMegaEvolve(Lcom/pixelmonmod/pixelmon/client/gui/battles/PixelmonClientData;)Z",at=@At("HEAD"),cancellable=true)
    private void backport$greninjaStone(com.pixelmonmod.pixelmon.client.gui.battles.PixelmonClientData data,CallbackInfoReturnable<Boolean> cir){
        var party=com.pixelmonmod.pixelmon.client.storage.ClientStorageManager.party;if(data==null||party==null)return;
        for(var p:party.getAll())if(p!=null&&p.getUUID().equals(data.pokemonUUID)&&local.pixelmon.backport.PermanentAsh.blockedOrdinaryStone(p)){cir.setReturnValue(false);return;}
    }
    @Inject(method={"canMegaEvolve()Z","canDynamax()Z","canUltraBurst()Z","canUseZMove()Z"},at=@At("HEAD"),cancellable=true)
    private void backport$onlyTera(CallbackInfoReturnable<Boolean> cir){
        ClientBattleManager manager=(ClientBattleManager)(Object)this;
        if(TeraRaidGimmicks.restricted(manager)){TeraRaidGimmicks.clear(manager);cir.setReturnValue(false);}
    }
}
