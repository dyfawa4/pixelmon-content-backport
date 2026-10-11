package local.pixelmon.backport.mixin;
import local.pixelmon.backport.NpcGimmicks;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=TrainerParticipant.class,remap=false)
public abstract class NpcGimmickMixin {
    @Inject(method="startBattle",at=@At("HEAD"))
    private void backport$prepare(CallbackInfo ci){NpcGimmicks.prepare((TrainerParticipant)(Object)this);}
    @Inject(method="getMove",at=@At("RETURN"))
    private void backport$plan(PixelmonWrapper p,CallbackInfoReturnable<MoveChoice> cir){
        if(NpcGimmicks.enabled(((TrainerParticipant)(Object)this).trainer))NpcGimmicks.plan(p,cir.getReturnValue());
    }
}
