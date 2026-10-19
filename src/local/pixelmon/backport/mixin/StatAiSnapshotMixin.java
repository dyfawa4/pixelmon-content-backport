package local.pixelmon.backport.mixin;

import java.util.List;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.StatsEffect;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Preserve all target snapshots for opponent effects; self effects need one. */
@Mixin(value=StatsEffect.class,remap=false)
public abstract class StatAiSnapshotMixin {
    @Redirect(method="weightStatsEffect",at=@At(value="INVOKE",target="Ljava/util/List;size()I"),require=9,allow=9)
    private int backport$statSnapshotSize(List<?> values,PixelmonWrapper user,MoveChoice choice,
            List<MoveChoice> choices,List<MoveChoice> bestChoices,List<MoveChoice> opponents,List<MoveChoice> bestOpponents){
        return ((StatsEffect)(Object)this).getUser()&&values==choice.targets?1:values.size();
    }
}
