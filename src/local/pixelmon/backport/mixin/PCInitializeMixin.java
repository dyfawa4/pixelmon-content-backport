package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.storage.PCStorage;
import local.pixelmon.backport.client.PCStorageSync;
import net.minecraftforge.fml.network.NetworkEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="com.pixelmonmod.pixelmon.comm.packetHandlers.clientStorage.newStorage.pc.ClientInitializePCPacket",remap=false)
public abstract class PCInitializeMixin {
    @Shadow private PCStorage storage;
    @Inject(method="handlePacket",at=@At("HEAD"))
    private void backport$reuse(NetworkEvent.Context context,CallbackInfo ci){storage=PCStorageSync.initialize(storage);}
}
