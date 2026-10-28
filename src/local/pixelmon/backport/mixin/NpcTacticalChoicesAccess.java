package local.pixelmon.backport.mixin;
import java.util.List;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
/** Reuse the complete native tactical pool, including status and submitted-action corrections. */
@Mixin(value=TacticalAI.class,remap=false)
public interface NpcTacticalChoicesAccess {
    @Invoker("getWeightedAttackChoices") List<MoveChoice> backport$weightedChoices(PixelmonWrapper own);
}
