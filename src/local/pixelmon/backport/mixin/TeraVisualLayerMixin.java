package local.pixelmon.backport.mixin;
import local.pixelmon.backport.client.TeraVisualRenderer;
import com.pixelmonmod.pixelmon.client.render.entity.layers.PokemonShimLayer;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractClientEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=PokemonShimLayer.class,remap=false)
public abstract class TeraVisualLayerMixin {
    @Inject(method="renderPixelmon(Lcom/pixelmonmod/pixelmon/entities/pixelmon/AbstractClientEntity;FFFFFFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;IZI)V",at=@At("HEAD"))
    private void backport$begin(AbstractClientEntity entity,float swing,float amount,float age,float yaw,float pitch,float partial,MatrixStack matrix,IRenderTypeBuffer buffer,int light,boolean pokedex,int pass,CallbackInfo ci){
        TeraVisualRenderer.begin(entity,buffer,partial,pokedex,pass);
    }
    @Inject(method="renderPixelmon(Lcom/pixelmonmod/pixelmon/entities/pixelmon/AbstractClientEntity;FFFFFFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;IZI)V",at=@At("RETURN"))
    private void backport$end(AbstractClientEntity entity,float swing,float amount,float age,float yaw,float pitch,float partial,MatrixStack matrix,IRenderTypeBuffer buffer,int light,boolean pokedex,int pass,CallbackInfo ci){
        TeraVisualRenderer.finish(matrix,light);
        local.pixelmon.backport.client.TeraTransformation.beams((TeraEvolutionBeamsInvoker)(Object)this,entity,matrix,buffer,partial);
    }
}
