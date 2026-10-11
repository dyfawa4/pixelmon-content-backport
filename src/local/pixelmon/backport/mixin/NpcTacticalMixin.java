package local.pixelmon.backport.mixin;
import java.util.List;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=TacticalAI.class,remap=false)
public abstract class NpcTacticalMixin {
    @Inject(method="getWeightedAttackChoices",at=@At("RETURN"))
    private void backport$context(PixelmonWrapper own,CallbackInfoReturnable<List<MoveChoice>> cir){
        local.pixelmon.backport.NpcTactics.weight(own,cir.getReturnValue());
    }
}
