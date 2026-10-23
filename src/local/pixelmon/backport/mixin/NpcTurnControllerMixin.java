package local.pixelmon.backport.mixin;
import local.pixelmon.backport.NpcTurnKnowledge;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=BattleController.class,remap=false)
public abstract class NpcTurnControllerMixin {
    @Inject(method="update",at=@At("HEAD"))
    private void backport$submittedActions(CallbackInfo ci){NpcTurnKnowledge.ready((BattleController)(Object)this);}
}
