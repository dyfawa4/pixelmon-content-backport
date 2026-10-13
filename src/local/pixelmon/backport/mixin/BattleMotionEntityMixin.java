package local.pixelmon.backport.mixin;
import local.pixelmon.backport.BattleMotion;
import net.minecraft.entity.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=Entity.class,remap=false)
public abstract class BattleMotionEntityMixin {
    @Shadow @Final private static DataParameter<Boolean> field_189655_aD;
    @Inject(method="func_213315_a",at=@At("HEAD"),cancellable=true)
    private void backport$holdMovement(MoverType type,Vector3d delta,CallbackInfo ci){Entity self=(Entity)(Object)this;if(BattleMotion.heldMovement(self)){BattleMotion.stop(self);ci.cancel();}}
    @Inject(method="func_189652_ae",at=@At("RETURN"),cancellable=true)
    private void backport$noGravity(CallbackInfoReturnable<Boolean> cir){if(BattleMotion.heldMovement((Entity)(Object)this))cir.setReturnValue(true);}
    @Redirect(method="func_189511_e",at=@At(value="INVOKE",target="Lnet/minecraft/entity/Entity;func_189652_ae()Z"))
    private boolean backport$saveOriginalGravity(Entity entity){return entity.func_184212_Q().func_187225_a(field_189655_aD);}
}
