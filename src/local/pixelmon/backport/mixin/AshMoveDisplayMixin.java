package local.pixelmon.backport.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.attacks.*;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import com.pixelmonmod.pixelmon.client.gui.pokechecker.PokeCheckerMovesScreen;
import com.pixelmonmod.pixelmon.client.gui.pokechecker.PokeCheckerScreen;
import com.pixelmonmod.pixelmon.api.storage.PokemonStorage;
import com.pixelmonmod.pixelmon.api.storage.StoragePosition;
import net.minecraft.client.gui.screen.Screen;
import local.pixelmon.backport.client.AshMoveDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=PokeCheckerMovesScreen.class,remap=false)
public abstract class AshMoveDisplayMixin extends PokeCheckerScreen {
    private AshMoveDisplayMixin(PokemonStorage storage,StoragePosition position,Screen parent){super(storage,position,parent);}
    @Redirect(method="drawMoveInfo",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/attacks/ImmutableAttack;getBasePower()I"),require=3,allow=3)
    private int backport$ashPower(ImmutableAttack move){return AshMoveDisplay.eligible(pokemon,move)?20:move.getBasePower();}
    @Redirect(method="drawMoveInfo",at=@At(value="INVOKE",target="Lnet/minecraft/client/resources/I18n;func_135052_a(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"))
    private String backport$ashDescription(String key,Object[] args,MatrixStack matrix,Attack attack){
        if("attack.water_shuriken.description".equals(key) && AshMoveDisplay.eligible(pokemon,attack.getActualMove())){
            String text=AshMoveDisplay.description(Minecraft.func_71410_x().field_71474_y.field_74363_ab);
            if(text!=null)return text;
        }
        return I18n.func_135052_a(key,args);
    }
    @Redirect(method="drawMoveInfo",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/client/gui/ScreenHelper;drawSplitString(Lcom/mojang/blaze3d/matrix/MatrixStack;Ljava/lang/String;IIII)V"),require=1,allow=1)
    private void backport$wrapDescription(MatrixStack matrix,String text,int x,int y,int width,int color,MatrixStack original,Attack attack){
        if(AshMoveDisplay.eligible(pokemon,attack.getActualMove()))AshMoveDisplay.drawDescription(matrix,text,x,y,width,color);
        else ScreenHelper.drawSplitString(matrix,text,x,y,width,color);
    }
}
