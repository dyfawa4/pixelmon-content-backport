package local.pixelmon.backport.mixin;
import java.time.*;
import com.pixelmonmod.pixelmon.quests.QuestProgress;
import com.pixelmonmod.pixelmon.quests.objectives.Objective;
import com.pixelmonmod.pixelmon.quests.quest.*;
import com.pixelmonmod.pixelmon.storage.playerData.QuestData;
import local.pixelmon.backport.NpcQuestConditions;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="com.pixelmonmod.pixelmon.quests.objectives.objectives.meta.DateObjective",remap=false)
public abstract class NpcQuestDateMixin {
    @Shadow @Final private static ZoneId ZONE_ID;
    @Inject(method="test",at=@At("HEAD"),cancellable=true)
    private void backport$date(Stage s,QuestData d,QuestProgress p,Objective o,Arguments a,Context c,CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(NpcQuestConditions.date(LocalDate.now(ZONE_ID),a.value(0,p),a.value(1,p)));
    }
}
