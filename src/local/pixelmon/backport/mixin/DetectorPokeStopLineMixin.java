package local.pixelmon.backport.mixin;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import local.pixelmon.backport.client.DetectorLineVisibility;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo @Mixin(targets="pixelmondetector.marker.MarkerManager", remap=false)
public abstract class DetectorPokeStopLineMixin {
    @Inject(method="drawLine", at=@At("HEAD"), cancellable=true)
    private void backport$hideClaimedPokeStop(Matrix4f matrix, @Coerce Object marker, IVertexBuilder output, RayTraceResult ray, CallbackInfo ci) {
        if (marker instanceof DetectorLineVisibility && ((DetectorLineVisibility) marker).backport$hideLine()) ci.cancel();
    }
}
