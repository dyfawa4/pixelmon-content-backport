package local.pixelmon.backport.mixin;
import java.util.ArrayList;
import com.pixelmonmod.pixelmon.api.dialogue.Choice;
import com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper;
import com.pixelmonmod.pixelmon.comm.packetHandlers.dialogue.DialogueNextActionPacket;
import net.minecraftforge.fml.network.NetworkEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="com.pixelmonmod.pixelmon.comm.packetHandlers.dialogue.DialogueChoiceMadePacket",remap=false)
public abstract class DialogueChoiceGuardMixin {
    @Shadow private Choice choice;
    @Inject(method="handlePacket",at=@At("HEAD"),cancellable=true)
    private void backport$currentChoice(NetworkEvent.Context context,CallbackInfo ci){
        var player=context.getSender();if(player==null){ci.cancel();return;}
        var choices=Choice.handleMap.get(player.func_110124_au());
        if(choice==null||choices==null||!choices.containsKey(choice.choiceID)){
            NetworkHelper.sendPacket(player,new DialogueNextActionPacket(DialogueNextActionPacket.DialogueGuiAction.CLOSE,new ArrayList<>()));ci.cancel();
        }
    }
}
