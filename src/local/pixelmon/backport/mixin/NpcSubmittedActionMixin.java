package local.pixelmon.backport.mixin;
import local.pixelmon.backport.NpcTurnKnowledge;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=PixelmonWrapper.class,remap=false)
public abstract class NpcSubmittedActionMixin {
    @Inject(method="selectAIAction",at=@At("HEAD"),cancellable=true)
    private void backport$defer(CallbackInfo ci){if(NpcTurnKnowledge.defer((PixelmonWrapper)(Object)this))ci.cancel();}
    @Inject(method="chooseMove(Lcom/pixelmonmod/pixelmon/battles/controller/ai/MoveChoice;)V",at=@At("HEAD"),cancellable=true)
    private void backport$medicineBeforeSwitch(MoveChoice choice,CallbackInfo ci){
        PixelmonWrapper own=(PixelmonWrapper)(Object)this;
        if(choice!=null&&!choice.isAttack()&&NpcTurnKnowledge.enabled(own)&&NpcTurnKnowledge.active(own.bc)
            &&local.pixelmon.backport.NpcMedicine.plan(own,choice)){
            own.attack=null;own.selectedAttack=null;own.newPokemonUUID=null;own.wait=false;ci.cancel();
        }
    }
}
