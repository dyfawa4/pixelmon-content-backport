package local.pixelmon.backport.mixin;
import local.pixelmon.backport.DisplayProtection;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="com.pixelmonmod.pixelmon.comm.packetHandlers.statueEditor.PokeDisplaySwapPacket",remap=false)
public abstract class PokeDisplaySwapGuardMixin {
 @Shadow private BlockPos pos;@Shadow private int newSlot;
 @Inject(method="handlePacket",at=@At("HEAD"),cancellable=true)
 private void backport$validate(NetworkEvent.Context context,CallbackInfo ci){if(!DisplayProtection.allow(context.getSender(),pos,newSlot))ci.cancel();}
}
