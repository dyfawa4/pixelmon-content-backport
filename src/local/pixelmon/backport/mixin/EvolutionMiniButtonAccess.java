package local.pixelmon.backport.mixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
/** The existing native popup row supplies its hover art and original font placement. */
@Mixin(targets="com.pixelmonmod.pixelmon.client.gui.widgets.PokeCheckerButton$MiniButton",remap=false)
public interface EvolutionMiniButtonAccess {
    @Invoker("render") void backport$render(MatrixStack matrix,int x,int y,float tick);
    @Invoker("drawText") void backport$drawText(MatrixStack matrix);
}
