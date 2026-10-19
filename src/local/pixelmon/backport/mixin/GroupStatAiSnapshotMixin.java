package local.pixelmon.backport.mixin;

import java.util.List;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.StatsEffect;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.StatsEffectCone;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.StatsEffectTeam;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** These legacy subclasses duplicate the self-snapshot bug in their own evaluator. */
@Mixin(value={StatsEffectCone.class,StatsEffectTeam.class},remap=false)
public abstract class GroupStatAiSnapshotMixin {
    @Redirect(method="weightEffect",at=@At(value="INVOKE",target="Ljava/util/List;size()I"),require=9,allow=9)
    private int backport$groupStatSnapshotSize(List<?> values,PixelmonWrapper user,MoveChoice choice,
            List<MoveChoice> choices,List<MoveChoice> bestChoices,List<MoveChoice> opponents,List<MoveChoice> bestOpponents){
        return ((StatsEffect)(Object)this).getUser()&&values==choice.targets?1:values.size();
    }
}
