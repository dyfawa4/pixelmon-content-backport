package local.pixelmon.backport.mixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.gui.battles.PixelmonClientData;
import com.pixelmonmod.pixelmon.client.gui.battles.pokemonOverlays.OpponentElement;
import com.pixelmonmod.pixelmon.client.gui.battles.pokemonOverlays.RaidElement;
import com.pixelmonmod.pixelmon.client.gui.widgets.PixelmonWidget;
import local.pixelmon.backport.ErosionClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=OpponentElement.class,remap=false)
public abstract class ErosionOpponentBadgeMixin extends PixelmonWidget {
    @Shadow public abstract PixelmonClientData getEnemy();
    @Inject(method="drawElement",at=@At("RETURN"))
    private void backport$erosionBadge(MatrixStack m,float scale,CallbackInfo ci){ErosionClient.opponent(m,getEnemy(),x,y,zLevel,false);}
}
