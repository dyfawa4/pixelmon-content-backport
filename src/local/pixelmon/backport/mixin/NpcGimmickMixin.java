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
        for(var side:participant.bc.participants)for(PixelmonWrapper member:side.allPokemon)backport$previewFlags.put(member,new boolean[]{member.onBattlefield,member.switchedThisTurn,member.takenTurn,member.hasBeenInBattle,member.wait,member.canAttack});
    }
    @Inject(method="startBattle",at=@At("HEAD"))
    private void backport$prepare(CallbackInfo ci){
        TrainerParticipant side=(TrainerParticipant)(Object)this;
        NpcGimmicks.prepare(side);
        local.pixelmon.backport.AuthorPiercing.prepare(side);
        // Nature recalculates stored-level stats. Finish equipment and nature
        // before native startBattle installs its temporary battle level.
        local.pixelmon.backport.TrainerItemClause.prepare(side);
    }
    @Inject(method="getMove",at=@At("RETURN"),cancellable=true)
    private void backport$plan(PixelmonWrapper p,CallbackInfoReturnable<MoveChoice> cir){
        // Native switch previews restore the controlled list, but leave these flags changed.
        // Planning must not actually take a Pokemon off the field or spend its turn.
        var flags=backport$previewFlags;backport$previewFlags=null;
        if(flags!=null)flags.forEach(NpcGimmickMixin::backport$restore);
        try{if(NpcGimmicks.enabled(((TrainerParticipant)(Object)this).trainer)){
            MoveChoice choice=local.pixelmon.backport.NpcTactics.refine(p,cir.getReturnValue());
            cir.setReturnValue(choice);NpcGimmicks.plan(p,choice);
        }}finally{
            // Candidate refinement also uses native previews. Preserve both teams'
            // real field/turn flags until the selected action is actually executed.
            if(flags!=null)flags.forEach(NpcGimmickMixin::backport$restore);
        }
    }
    @Unique private static void backport$restore(PixelmonWrapper member,boolean[] state){member.onBattlefield=state[0];member.switchedThisTurn=state[1];member.takenTurn=state[2];member.hasBeenInBattle=state[3];member.wait=state[4];member.canAttack=state[5];}
}
