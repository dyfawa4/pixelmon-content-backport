package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.client.ClientProxy;
import local.pixelmon.backport.client.BossCaptureClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets="com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.screens.bag.ChooseBag",remap=false)
public abstract class BossBagSelectionMixin {
    @Inject(method="func_231160_c_",at=@At("HEAD"))
    private void backport$restoreBagCategory(CallbackInfo ci){BossCaptureClient.prepare(ClientProxy.battleManager);}
    @Inject(method="click",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/client/gui/battles/ClientBattleManager;getBagItems(Lcom/pixelmonmod/pixelmon/api/battles/BagSection;)Ljava/util/List;"),cancellable=true)
    private void backport$recheckBall(int width,int height,double x,double y,CallbackInfo ci){
        if(BossCaptureClient.click(ClientProxy.battleManager))ci.cancel();
    }
}
