package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.quests.QuestProgress;
import com.pixelmonmod.pixelmon.quests.objectives.Objective;
import com.pixelmonmod.pixelmon.quests.quest.*;
import com.pixelmonmod.pixelmon.storage.playerData.QuestData;
import local.pixelmon.backport.NpcQuestLogic;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="com.pixelmonmod.pixelmon.quests.objectives.objectives.entity.NPCObjective", remap=false)
public abstract class NpcQuestSubmitMixin {
    @Shadow public abstract String identifier();
    @Inject(method="test",at=@At("HEAD"),cancellable=true)
    private void backport$submit(Stage s, QuestData d, QuestProgress p, Objective o, Arguments a, Context c, CallbackInfoReturnable<Boolean> cir) {
        if(identifier().equals("NPC_GIVE") || identifier().equals("NPC_SHOW")) cir.setReturnValue(NpcQuestLogic.npcTest(identifier(),d,p,a,c));
    }
}
