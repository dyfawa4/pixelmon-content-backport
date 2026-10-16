package local.pixelmon.backport.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.BattleBaseScreen;
import com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.ChooseAttack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A detached attack screen must not select a move from the overworld party. */
@Mixin(value=ChooseAttack.class,remap=false)
public abstract class BattleSelectionSafetyMixin extends BattleBaseScreen {
    protected BattleSelectionSafetyMixin(){super(null,null);}
    @Unique private boolean backport$detached(){
        return bm==null || bm.battleEnded || bm.battleControllerIndex<0
            || bm.teamPokemon==null || bm.fullOurPokemon==null;
    }
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void backport$renderLiveBattle(MatrixStack matrix,int width,int height,int x,int y,float partial,CallbackInfo ci){
        if(backport$detached())ci.cancel();
    }
    @Inject(method="click",at=@At("HEAD"),cancellable=true)
    private void backport$clickLiveBattle(int width,int height,double x,double y,CallbackInfo ci){
        if(backport$detached())ci.cancel();
    }
}
