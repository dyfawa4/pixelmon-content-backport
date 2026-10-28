package local.pixelmon.backport.mixin;
import java.util.*;
import com.pixelmonmod.pixelmon.battles.controller.ai.AggressiveAI;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.NpcTurnKnowledge;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Native reserve selection is also used directly by pivots, outside TrainerParticipant.getMove. */
@Mixin(value=AggressiveAI.class,remap=false)
public abstract class NpcSwitchPreviewMixin {
    @Unique private final Deque<Map<PixelmonWrapper,boolean[]>> backport$replacementFrames=new ArrayDeque<>();
    @Inject(method="getNextSwitch",at=@At("HEAD"))
    private void backport$before(PixelmonWrapper own,CallbackInfoReturnable<UUID> cir){
        Map<PixelmonWrapper,boolean[]> flags=new IdentityHashMap<>();
        if(own.bc!=null&&!own.bc.simulateMode&&NpcTurnKnowledge.enabled(own))
            for(var side:own.bc.participants)for(var member:side.allPokemon)flags.put(member,new boolean[]{member.onBattlefield,member.switchedThisTurn,member.takenTurn,member.hasBeenInBattle,member.wait,member.canAttack});
        backport$replacementFrames.push(flags);
    }
    @Inject(method="getNextSwitch",at=@At("RETURN"))
    private void backport$after(PixelmonWrapper own,CallbackInfoReturnable<UUID> cir){
        backport$replacementFrames.pop().forEach((member,state)->{member.onBattlefield=state[0];member.switchedThisTurn=state[1];member.takenTurn=state[2];member.hasBeenInBattle=state[3];member.wait=state[4];member.canAttack=state[5];});
    }
}
