package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.quests.QuestProgress;
import com.pixelmonmod.pixelmon.quests.objectives.Objective;
import com.pixelmonmod.pixelmon.quests.quest.*;
import com.pixelmonmod.pixelmon.storage.playerData.QuestData;
import local.pixelmon.backport.NpcQuestConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="com.pixelmonmod.pixelmon.quests.objectives.objectives.world.WorldTimeObjective",remap=false)
public abstract class NpcQuestTimeMixin {
    @Inject(method="test",at=@At("HEAD"),cancellable=true)
    private void backport$time(Stage s,QuestData d,QuestProgress p,Objective o,Arguments a,Context c,CallbackInfoReturnable<Boolean> cir) {
        if(d.getPlayer()==null) {cir.setReturnValue(false);return;}
        cir.setReturnValue(NpcQuestConditions.time(d.getPlayer().func_71121_q().func_72820_D(),a.value(0,p),a.size()>1?a.value(1,p):null));
    }
}
