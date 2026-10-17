package local.pixelmon.backport.mixin;
import local.pixelmon.backport.RidingMotion;
import net.minecraftforge.fml.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets={"com.pixelmonmod.pixelmon.comm.packetHandlers.RidingMovementPacket","com.pixelmonmod.pixelmon.comm.packetHandlers.RidingVerticalMovementPacket"},remap=false)
public abstract class RidingPacketGuardMixin {
    @Inject(method="handlePacket",at=@At("HEAD"),cancellable=true)
    private void backport$ignoreOldInputs(NetworkEvent.Context context,CallbackInfo ci){var p=context.getSender();if(p!=null&&RidingMotion.active(p.func_184187_bx()))ci.cancel();}
}
