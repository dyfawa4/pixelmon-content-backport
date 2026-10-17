package local.pixelmon.backport.mixin;
import local.pixelmon.backport.client.RidingSeat;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ActiveRenderInfo.class,remap=false)
public abstract class RidingCameraMixin {
    @Shadow public abstract Vector3d func_216785_c();
    @Shadow protected abstract void func_216774_a(Vector3d position);
    @Inject(method="func_216772_a",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/ActiveRenderInfo;func_216775_b(DDD)V",shift=At.Shift.AFTER,ordinal=0))
    private void backport$visualPivot(IBlockReader world,Entity rider,boolean third,boolean reverse,float partial,CallbackInfo ci){
        Vector3d offset=RidingSeat.cameraOffset(rider,partial,third);if(offset.func_189985_c()<1e-7)return;
        Vector3d from=func_216785_c(),to=from.func_178787_e(offset);
        RayTraceResult hit=rider.field_70170_p.func_217299_a(new RayTraceContext(from,to,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,rider));
        if(hit.func_216346_c()!=RayTraceResult.Type.MISS)to=hit.func_216347_e().func_178788_d(offset.func_72432_b().func_186678_a(.06));
        func_216774_a(to);
    }
}
