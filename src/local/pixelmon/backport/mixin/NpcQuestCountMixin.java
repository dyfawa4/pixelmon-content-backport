package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.quests.QuestProgress;
import com.pixelmonmod.pixelmon.quests.objectives.Objective;
import com.pixelmonmod.pixelmon.quests.quest.Stage;
import com.pixelmonmod.pixelmon.storage.playerData.QuestData;
import local.pixelmon.backport.NpcQuestLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Objective.class)
public abstract class NpcQuestCountMixin {
    @Inject(method="receive",at=@At("HEAD"),remap=false)
    private void backport$legacyDefaultQuantity(String id,Stage s,QuestData d,QuestProgress p,int index,Object[] args,CallbackInfoReturnable<Boolean> cir) {
        Objective o=(Objective)(Object)this;
        if(!(o.getInternalObjective() instanceof com.pixelmonmod.pixelmon.quests.objectives.objectives.player.ItemObjective))return;
        NpcQuestProgressAccess access=(NpcQuestProgressAccess)(Object)p;
        int[] left=access.backport$remaining(),total=access.backport$totals();
        if(index>=0 && index<left.length && index<total.length && total[index]<1 && left[index]<0)left[index]=total[index]=1;
    }
    @Redirect(method="receive", at=@At(value="INVOKE", target="Lcom/pixelmonmod/pixelmon/quests/QuestProgress;completeObjective(I)V"), remap=false)
    private void backport$count(QuestProgress p, int index, String id, Stage stage, QuestData data, QuestProgress progress, int objectiveIndex, Object[] args) {
        NpcQuestLogic.complete(p,index,(Objective)(Object)this,args);
    }
    @Redirect(method="receive", at=@At(value="INVOKE", target="Lcom/pixelmonmod/pixelmon/quests/QuestProgress;resetObjective(I)V"), remap=false)
    private void backport$keepSuccessfulSubmission(QuestProgress p,int index) {
        Objective o=(Objective)(Object)this;
        String id=o.getInternalObjective().identifier();
        if((id.equals("NPC_GIVE") || id.equals("NPC_SHOW")) && Boolean.TRUE.equals(o.getData().value(2,p)))return;
        p.resetObjective(index);
    }
}
