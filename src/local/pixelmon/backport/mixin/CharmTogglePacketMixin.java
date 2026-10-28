package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.comm.packetHandlers.SetCharmPacket;
import com.pixelmonmod.pixelmon.enums.EnumFeatureState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A client toggle may enable/disable an owned charm, never revoke ownership. */
@Mixin(value=SetCharmPacket.class,remap=false)
public abstract class CharmTogglePacketMixin {
    @Shadow private EnumFeatureState state;
    @Inject(method="handlePacket",at=@At("HEAD"),cancellable=true)
    private void backport$owned(net.minecraftforge.fml.network.NetworkEvent.Context context,CallbackInfo ci){if(state==EnumFeatureState.Disabled)ci.cancel();}
}
