package local.pixelmon.backport.mixin;
import java.util.List;
import local.pixelmon.backport.NpcTurnKnowledge;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.ai.BattleAIBase;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=BattleAIBase.class,remap=false)
public abstract class NpcKnownActionMixin {
    @Inject(method="getMoveset",at=@At("HEAD"),cancellable=true)
    private void backport$submittedMoves(PixelmonWrapper enemy,CallbackInfoReturnable<List<Attack>> cir){List<Attack> moves=NpcTurnKnowledge.moves(enemy);if(moves!=null)cir.setReturnValue(moves);}
}
