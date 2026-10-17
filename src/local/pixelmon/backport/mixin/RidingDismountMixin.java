package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractMovesEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import local.pixelmon.backport.RidingMotion;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=AbstractMovesEntity.class,remap=false)
public abstract class RidingDismountMixin {
    @Inject(method="func_184225_p",at=@At("RETURN"))
    private void backport$clearImmediately(Entity passenger,CallbackInfo ci){
        if(!((Object)this instanceof PixelmonEntity))return;
        PixelmonEntity e=(PixelmonEntity)(Object)this;RidingMotion.clear(e);
        // Vanilla prevents boarding anything for 60 ticks after dismounting.
        // Owned Pokemon support immediate switching; the native interaction
        // still checks rideability, ownership, configuration and both events.
        if(passenger instanceof net.minecraft.entity.player.PlayerEntity&&passenger.func_110124_au().equals(e.func_184780_dh()))
            ((RidingCooldownAccess)passenger).backport$boardingCooldown(0);
    }
}
