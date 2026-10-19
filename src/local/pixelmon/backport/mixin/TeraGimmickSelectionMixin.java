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
    @Inject(method="render",at=@At("HEAD"))
    private void backport$clearRender(com.mojang.blaze3d.matrix.MatrixStack matrix,int width,int height,int x,int y,float partial,CallbackInfo ci){TeraRaidGimmicks.clear(ClientProxy.battleManager);}
    @Inject(method="click",at=@At("HEAD"))
    private void backport$clearClick(int width,int height,double x,double y,CallbackInfo ci){TeraRaidGimmicks.clear(ClientProxy.battleManager);}
    @Inject(method="click",at=@At("RETURN"))
    private void backport$nativeChoice(int width,int height,double x,double y,CallbackInfo ci){
        ClientBattleManager bm=ClientProxy.battleManager;
        if(TeraRaidGimmicks.restricted(bm)){TeraRaidGimmicks.clear(bm);return;}
        if(bm==null || !(bm.megaEvolving || bm.dynamaxing || bm.showZMoves))return;
        PixelmonClientData p=bm.getCurrentPokemon();
        if(p!=null)TeraNetwork.CHANNEL.sendToServer(new TeraNetwork.Request(bm.battleControllerIndex,p.pokemonUUID,4,-1));
    }
}
