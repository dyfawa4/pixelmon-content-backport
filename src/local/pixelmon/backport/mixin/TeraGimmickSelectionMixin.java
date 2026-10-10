package local.pixelmon.backport.mixin;

import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.client.ClientProxy;
import com.pixelmonmod.pixelmon.client.gui.battles.*;
import com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.ChooseAttack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Selecting a native gimmick replaces a queued Tera selection. */
@Mixin(value=ChooseAttack.class,remap=false)
public abstract class TeraGimmickSelectionMixin {
    @Inject(method="click",at=@At("RETURN"))
    private void backport$nativeChoice(int width,int height,double x,double y,CallbackInfo ci){
        ClientBattleManager bm=ClientProxy.battleManager;
        if(bm==null || !(bm.megaEvolving || bm.dynamaxing || bm.showZMoves))return;
        PixelmonClientData p=bm.getCurrentPokemon();
        if(p!=null)TeraNetwork.CHANNEL.sendToServer(new TeraNetwork.Request(bm.battleControllerIndex,p.pokemonUUID,4,-1));
    }
}
