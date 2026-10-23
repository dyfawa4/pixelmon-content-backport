package local.pixelmon.backport.mixin;
import java.util.function.Supplier;
import net.minecraftforge.fml.network.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="net.blay09.mods.trashslot.network.MessageTrashSlotContent",remap=false)
public abstract class TrashSlotHandshakeMixin {
    @Inject(method="handle",at=@At("RETURN"))
    private static void backport$serverPresent(@Coerce Object message,Supplier<NetworkEvent.Context> supplier,CallbackInfo ci){
        NetworkEvent.Context context=supplier.get();
        if(context.getDirection()==NetworkDirection.PLAY_TO_CLIENT)context.enqueueWork(local.pixelmon.backport.TrashSlotCompat::received);
    }
}
