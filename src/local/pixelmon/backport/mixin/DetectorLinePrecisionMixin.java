package local.pixelmon.backport.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import local.pixelmon.backport.client.DetectorLineCoordinates;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo @Mixin(targets="pixelmondetector.marker.MarkerManager",remap=false)
public abstract class DetectorLinePrecisionMixin {
    @Redirect(method="drawAllLines",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/matrix/MatrixStack;func_227861_a_(DDD)V"))
    private void backport$relativeMatrix(MatrixStack stack,double x,double y,double z){
        // The vertices now carry their own camera-relative offset.
    }
    @Redirect(method="drawLine",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/IVertexBuilder;func_227888_a_(Lnet/minecraft/util/math/vector/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/IVertexBuilder;",ordinal=0))
    private IVertexBuilder backport$lineOrigin(IVertexBuilder builder,Matrix4f matrix,float x,float y,float z,Matrix4f original,@Coerce Object marker,IVertexBuilder output,RayTraceResult ray){
        return backport$vertex(builder,matrix,ray.func_216347_e());
    }
    @Redirect(method="drawLine",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/IVertexBuilder;func_227888_a_(Lnet/minecraft/util/math/vector/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/IVertexBuilder;",ordinal=1))
    private IVertexBuilder backport$lineTarget(IVertexBuilder builder,Matrix4f matrix,float x,float y,float z,Matrix4f original,@Coerce Object marker,IVertexBuilder output,RayTraceResult ray){
        return backport$vertex(builder,matrix,DetectorLineCoordinates.position(marker));
    }
    private static IVertexBuilder backport$vertex(IVertexBuilder builder,Matrix4f matrix,Vector3d position){
        Vector3d camera=Minecraft.func_71410_x().field_71460_t.func_215316_n().func_216785_c();
        return builder.func_227888_a_(matrix,DetectorLineCoordinates.relative(position.field_72450_a,camera.field_72450_a),DetectorLineCoordinates.relative(position.field_72448_b,camera.field_72448_b),DetectorLineCoordinates.relative(position.field_72449_c,camera.field_72449_c));
    }
}
