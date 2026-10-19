package local.pixelmon.backport.mixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.gui.battles.PixelmonClientData;
import com.pixelmonmod.pixelmon.client.gui.battles.pokemonOverlays.AllyElement;
import com.pixelmonmod.pixelmon.client.gui.widgets.PixelmonWidget;
import local.pixelmon.backport.ErosionClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=AllyElement.class,remap=false)
public abstract class ErosionAllyBadgeMixin extends PixelmonWidget {
    @Shadow public abstract PixelmonClientData getAlly();
    @Inject(method="drawElement",at=@At("RETURN"))
    private void backport$erosionBadge(MatrixStack m,float scale,CallbackInfo ci){ErosionClient.ally(m,getAlly(),x,y,zLevel);}
}
