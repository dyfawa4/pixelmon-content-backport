package local.pixelmon.backport.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(targets="com.pixelmonmod.pixelmon.quests.QuestProgress", remap=false)
public interface NpcQuestProgressAccess {
    @Accessor("objectiveCompletion") int[] backport$remaining();
    @Accessor("objectiveCompletionReference") int[] backport$totals();
}
