package local.pixelmon.backport.mixin;
import local.pixelmon.backport.NpcQuestEvents;
import com.pixelmonmod.pixelmon.quests.exceptions.InvalidQuestArgsException;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="com.pixelmonmod.pixelmon.quests.listeners.ItemListeners",remap=false)
public abstract class NpcQuestInventoryMixin {
    @Inject(method="onPlayerTick",at=@At("HEAD"),cancellable=true)
    private void backport$inventory(TickEvent.PlayerTickEvent e,CallbackInfo ci)throws InvalidQuestArgsException {
        NpcQuestEvents.poll(e);ci.cancel();
    }
}
