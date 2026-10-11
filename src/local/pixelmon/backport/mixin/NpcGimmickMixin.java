package local.pixelmon.backport.mixin;
import local.pixelmon.backport.NpcGimmicks;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=TrainerParticipant.class,remap=false)
public abstract class NpcGimmickMixin {
    @Unique private java.util.Map<PixelmonWrapper,boolean[]> backport$previewFlags;
    @Inject(method="getMove",at=@At("HEAD"))
    private void backport$savePreview(PixelmonWrapper p,CallbackInfoReturnable<MoveChoice> cir){
        TrainerParticipant participant=(TrainerParticipant)(Object)this;
        backport$previewFlags=null;
        if(participant.bc==null || participant.bc.simulateMode || !NpcGimmicks.enabled(participant.trainer))return;
        backport$previewFlags=new java.util.IdentityHashMap<>();
        for(PixelmonWrapper member:participant.allPokemon)backport$previewFlags.put(member,new boolean[]{member.onBattlefield,member.switchedThisTurn,member.takenTurn,member.hasBeenInBattle});
    }
    @Inject(method="startBattle",at=@At("HEAD"))
    private void backport$prepare(CallbackInfo ci){NpcGimmicks.prepare((TrainerParticipant)(Object)this);}
    @Inject(method="getMove",at=@At("RETURN"),cancellable=true)
    private void backport$plan(PixelmonWrapper p,CallbackInfoReturnable<MoveChoice> cir){
        // Native switch previews restore the controlled list, but leave these flags changed.
        // Planning must not actually take a Pokemon off the field or spend its turn.
        if(backport$previewFlags!=null){
            backport$previewFlags.forEach((member,state)->{member.onBattlefield=state[0];member.switchedThisTurn=state[1];member.takenTurn=state[2];member.hasBeenInBattle=state[3];});
            backport$previewFlags=null;
        }
        if(NpcGimmicks.enabled(((TrainerParticipant)(Object)this).trainer)){
            MoveChoice choice=local.pixelmon.backport.NpcTactics.refine(p,cir.getReturnValue());
            cir.setReturnValue(choice);NpcGimmicks.plan(p,choice);
        }
    }
}
