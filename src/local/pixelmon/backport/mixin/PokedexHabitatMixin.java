package local.pixelmon.backport.mixin;

import local.pixelmon.backport.PokedexHabitats;
import com.pixelmonmod.pixelmon.api.pokedex.PokedexRegistrationStatus;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.comm.data.*;
import com.pixelmonmod.pixelmon.comm.packetHandlers.pokedex.*;
import net.minecraftforge.fml.network.NetworkEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve the native seen/caught gate and native packet/cache format. */
@Mixin(value=RequestSpawnDataPacket.class,remap=false)
public abstract class PokedexHabitatMixin extends AbstractPixelmonPacket {
    @Shadow private int dex;
    protected PokedexHabitatMixin(){super(PacketDirection.PLAY_SERVER,true);}
    @Inject(method="handlePacket",at=@At("HEAD"),cancellable=true)
    private void backport$habitat(NetworkEvent.Context context,CallbackInfo ci){
        if(!PokedexHabitats.handles(dex))return;
        var player=context.getSender();
        if(player==null){ci.cancel();return;}
        var status=StorageProxy.getParty(player).playerPokedex.get(dex);
        reply(context,new SendSpawnDataPacket(dex,status==PokedexRegistrationStatus.UNKNOWN?java.util.Collections.emptyList():PokedexHabitats.data(dex)));
        ci.cancel();
    }
}
