package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractMovesEntity;
import local.pixelmon.backport.RidingMotion;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Small growth can make the native moving seat's negative offset place the rider below the mount's feet. */
@Mixin(value=AbstractMovesEntity.class,remap=false)
public abstract class RidingPassengerSafetyMixin {
    @Inject(method="func_184232_k",at=@At("RETURN"))
    private void backport$aboveGround(Entity passenger,CallbackInfo ci){
        Entity mount=(Entity)(Object)this;
        if(!RidingMotion.active(mount)||mount.func_184179_bs()!=passenger)return;
        double floor=mount.func_226278_cu_()+.05;
        if(passenger.func_226278_cu_()<floor)passenger.func_70107_b(passenger.func_226277_ct_(),floor,passenger.func_226281_cx_());
    }
    @Redirect(method="func_225503_b_",at=@At(value="FIELD",target="Lcom/pixelmonmod/pixelmon/entities/pixelmon/AbstractMovesEntity;field_191988_bg:F"))
    private float backport$fallUsesVelocity(AbstractMovesEntity mount){
        // The new controller stores a unit direction here, not the legacy flight speed.
        // Preserve real vertical impact and all native fall checks, excluding this false horizontal impact.
        return RidingMotion.active(mount)?0:mount.field_191988_bg;
    }
}
