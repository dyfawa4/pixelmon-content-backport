package local.pixelmon.backport.mixin;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import com.pixelmonmod.pixelmon.entities.DenEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo @Mixin(targets="mcp.mobius.waila.overlay.RayTracing",remap=false)
public abstract class JadeDenTargetMixin {
 @Inject(method="canBeTarget",at=@At("HEAD"),cancellable=true)
 private void backport$visibleDen(Entity candidate,Entity viewer,CallbackInfoReturnable<Boolean> ci){if(candidate instanceof DenEntity&&candidate.func_70089_S()&&candidate!=viewer.func_184187_bx())ci.setReturnValue(true);}
 @Inject(method="rayTrace(Lnet/minecraft/entity/Entity;DF)Lnet/minecraft/util/math/RayTraceResult;",at=@At("RETURN"),cancellable=true)
 private void backport$model(Entity viewer,double reach,float partial,CallbackInfoReturnable<RayTraceResult> ci){ci.setReturnValue(local.pixelmon.backport.client.JadeDens.target(viewer,reach,partial,ci.getReturnValue()));}
}
