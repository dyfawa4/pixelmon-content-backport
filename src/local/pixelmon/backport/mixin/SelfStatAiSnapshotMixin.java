package local.pixelmon.backport.mixin;

import java.util.List;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.StatsEffectPost;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** A post-move stat change affects the user, regardless of the attack's target count. */
@Mixin(value=StatsEffectPost.class,remap=false)
public abstract class SelfStatAiSnapshotMixin {
    @Redirect(method="weightEffect",at=@At(value="INVOKE",target="Ljava/util/List;size()I"),require=9,allow=9)
    private int backport$selfSnapshotSize(List<?> values,PixelmonWrapper user,MoveChoice choice,
            List<MoveChoice> choices,List<MoveChoice> bestChoices,List<MoveChoice> opponents,List<MoveChoice> bestOpponents){
        // Native code initializes only snapshot[0] with the user's stats. All
        // six snapshot allocations must have one slot, even for spread moves.
        // Opponent/choice list lengths used elsewhere in this method stay real.
        return values==choice.targets?1:values.size();
    }
}
