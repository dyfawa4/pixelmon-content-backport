package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractMovesEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.movement.*;
import local.pixelmon.backport.RidingMotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value={PixelmonMovementGround.class,PixelmonMovementFlying.class},remap=false)
public abstract class RidingLegacyMotionMixin extends PixelmonMovementBase {
    protected RidingLegacyMotionMixin(AbstractMovesEntity e){super(e);}
    @Inject(method={"onLivingUpdate","handleMovement"},at=@At("HEAD"),cancellable=true)
    private void backport$singleController(CallbackInfo ci){if(RidingMotion.active(pixelmon))ci.cancel();}
}
