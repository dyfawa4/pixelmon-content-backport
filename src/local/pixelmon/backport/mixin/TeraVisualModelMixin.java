package local.pixelmon.backport.mixin;
import local.pixelmon.backport.client.TeraVisualRenderer;
import com.pixelmonmod.pixelmon.client.models.smd.ValveStudioModel;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ValveStudioModel.class,remap=false)
public abstract class TeraVisualModelMixin {
    @Inject(method="func_225598_a_",at=@At("RETURN"))
    private void backport$surface(MatrixStack matrix,IVertexBuilder vertices,int light,int overlay,float r,float g,float b,float alpha,CallbackInfo ci){
        TeraVisualRenderer.model((ValveStudioModel)(Object)this,matrix,light,overlay,alpha);
        local.pixelmon.backport.client.RidingSeat.capture((ValveStudioModel)(Object)this,matrix);
    }
}
