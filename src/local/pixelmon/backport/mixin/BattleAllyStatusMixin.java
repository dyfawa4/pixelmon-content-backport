package local.pixelmon.backport.mixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.gui.battles.PixelmonClientData;
import com.pixelmonmod.pixelmon.client.gui.battles.pokemonOverlays.AllyElement;
import com.pixelmonmod.pixelmon.client.gui.widgets.PixelmonWidget;
import local.pixelmon.backport.BattleStatusIcons;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=AllyElement.class,remap=false)
public abstract class BattleAllyStatusMixin extends PixelmonWidget {
    @Shadow public abstract PixelmonClientData getAlly();
    @Inject(method="drawElement",at=@At("RETURN"))
    private void backport$erosionBadge(MatrixStack m,float scale,CallbackInfo ci){BattleStatusIcons.ally(m,getAlly(),x,y,zLevel);}
}
