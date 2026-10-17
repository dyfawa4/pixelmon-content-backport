package local.pixelmon.backport.mixin;
import java.util.ArrayList;
import com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper;
import com.pixelmonmod.pixelmon.comm.packetHandlers.dialogue.DialogueNextActionPacket;
import com.pixelmonmod.pixelmon.quests.QuestProgress;
import com.pixelmonmod.pixelmon.quests.quest.*;
import com.pixelmonmod.pixelmon.storage.playerData.QuestData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="com.pixelmonmod.pixelmon.quests.actions.actions.EndDialogueAction",remap=false)
public abstract class QuestEndDialogueMixin {
    @Inject(method="execute",at=@At("TAIL"))
    private void backport$closeDialogue(Quest quest,Stage stage,QuestData data,QuestProgress progress,Arguments arguments,CallbackInfo ci){
        if(data.getPlayer()!=null)NetworkHelper.sendPacket(data.getPlayer(),new DialogueNextActionPacket(DialogueNextActionPacket.DialogueGuiAction.CLOSE,new ArrayList<>()));
    }
}
