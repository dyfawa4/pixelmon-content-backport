package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.client.render.entity.renderers.AbstractPokemonRenderer;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractClientEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import local.pixelmon.backport.client.RidingSeat;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=AbstractPokemonRenderer.class,remap=false)
public abstract class RidingModelSeatMixin {
    @Inject(method="render(Lcom/pixelmonmod/pixelmon/entities/pixelmon/AbstractClientEntity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",at=@At("HEAD"))
    private void backport$root(AbstractClientEntity e,float yaw,float partial,MatrixStack matrix,IRenderTypeBuffer buffer,int light,CallbackInfo ci){RidingSeat.begin(e,matrix,partial);}
    @Inject(method="render(Lcom/pixelmonmod/pixelmon/entities/pixelmon/AbstractClientEntity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",at=@At("RETURN"))
    private void backport$rootEnd(CallbackInfo ci){RidingSeat.end();}
}
