package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.client.gui.battles.ClientBattleManager;
import local.pixelmon.backport.client.BossCaptureClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Incomplete battle display data is pending, never permission to capture. */
@Mixin(value=ClientBattleManager.class,remap=false)
public abstract class ClientCaptureReadinessMixin {
    @Inject(method="canCatchOpponent()Z",at=@At("HEAD"),cancellable=true)
    private void backport$captureDataReady(CallbackInfoReturnable<Boolean> cir){
        if(!BossCaptureClient.ready((ClientBattleManager)(Object)this))cir.setReturnValue(false);
    }
}
