package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.client.render.entity.layers.PokemonShimLayer;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractClientEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=PokemonShimLayer.class,remap=false)
public interface TeraEvolutionBeamsInvoker {
    @Invoker("renderEvolutionLightBeams") void backport$teraEvolutionBeams(AbstractClientEntity entity,MatrixStack matrix,IRenderTypeBuffer buffer);
}
