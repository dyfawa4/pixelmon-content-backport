package local.pixelmon.backport.mixin;
import java.util.UUID;
import com.pixelmonmod.pixelmon.api.storage.StoragePosition;
import com.pixelmonmod.pixelmon.comm.EnumUpdateType;
import local.pixelmon.backport.client.StoragePacketGuard;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="com.pixelmonmod.pixelmon.comm.packetHandlers.clientStorage.newStorage.ClientSetPacket",remap=false)
public abstract class StoragePacketGuardMixin {
    @Shadow private UUID storageUUID;
    @Shadow private StoragePosition position;
    @Shadow private EnumUpdateType[] dataTypes;
    @Shadow private PacketBuffer pokemonData;
    @Inject(method="handlePacket",at=@At("HEAD"),cancellable=true)
    private void backport$completeIdentity(NetworkEvent.Context context,CallbackInfo ci){
        if(StoragePacketGuard.guard(storageUUID,position,pokemonData,dataTypes))ci.cancel();
    }
}
