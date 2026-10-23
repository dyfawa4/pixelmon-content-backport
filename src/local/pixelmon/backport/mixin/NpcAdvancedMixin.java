package local.pixelmon.backport.mixin;
import java.util.*;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import local.pixelmon.backport.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extend native legal switch previews with immediate and next-turn survival. */
@Mixin(value=AdvancedAI.class,remap=false)
public abstract class NpcAdvancedMixin {
    @Unique private PixelmonWrapper backport$anchor;
    @Unique private double backport$before;
    @Unique private int backport$slot;
    @Unique private final Map<UUID,Double> backport$health=new HashMap<>();
    @Unique private final Map<UUID,Float> backport$benefits=new HashMap<>();
    @Inject(method="getNextMove",at=@At("HEAD"))
    private void backport$start(PixelmonWrapper own,CallbackInfoReturnable<MoveChoice> cir){
        backport$anchor=null;backport$benefits.clear();backport$health.clear();backport$before=0;
        if(own!=null && own.bc!=null && !own.bc.simulateMode && !own.bc.isRaid()
            && own.getParticipant() instanceof TrainerParticipant && NpcGimmicks.enabled(((TrainerParticipant)own.getParticipant()).trainer)){
            backport$anchor=own;backport$slot=own.getParticipant().controlledPokemon.indexOf(own);
            for(var member:own.getParticipant().allPokemon)backport$health.put(member.getPokemonUUID(),(double)member.getHealth());
        }
    }
    @Inject(method="getNextOpponentMoves",at=@At("RETURN"),cancellable=true)
    private void backport$survival(PixelmonWrapper target,List<PixelmonWrapper> opponents,CallbackInfoReturnable<List<MoveChoice>> cir){
        PixelmonWrapper own=backport$anchor;if(own==null || !own.bc.simulateMode)return;
        // AdvancedAI passes the OUTGOING wrapper even after its simulated switch.
        // Read the current occupant of that slot to score the incoming member.
        var controlled=own.getParticipant().controlledPokemon;
        if(backport$slot<0||backport$slot>=controlled.size())return;target=controlled.get(backport$slot);
        var choices=NpcTurnKnowledge.restrict(target.bc,cir.getReturnValue());cir.setReturnValue(choices);
        // Native shortlist selection can omit the actually submitted move.
        double incoming;
        try(var action=NpcTurnKnowledge.preview(target,target==own?own.attack:null)){
            incoming=NpcTactics.incoming(target,NpcTurnKnowledge.active(own.bc)?NpcTactics.threats(target):choices);
        }
        if(NpcTurnKnowledge.active(own.bc))incoming=Math.max(incoming,NpcTactics.incoming(target,NpcTactics.threats(target,true))*.6);
        if(target==own)backport$before=incoming;
        else {
            double entry=Math.max(0,backport$health.getOrDefault(target.getPokemonUUID(),(double)target.getHealth())-target.getHealth());
            backport$benefits.put(target.getPokemonUUID(),NpcSwitchPolicy.adjustment(own.getHealth(),backport$before,target.getHealth(),incoming,
                own.getBattleStats().getSumIncreases(),target.getMaxHealth(),entry,NpcMedicine.value(own,own),NpcMedicine.value(target,own)));
        }
    }
    @Redirect(method="getNextMove",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/controller/ai/MoveChoice;raiseWeight(F)V"))
    private void backport$rewardSafeSwitch(MoveChoice choice,float weight){
        Float benefit=backport$benefits.remove(choice.switchPokemon);
        choice.raiseWeight(weight+(benefit==null?0:benefit));
    }
    @Inject(method="getNextMove",at=@At("RETURN"))
    private void backport$end(PixelmonWrapper own,CallbackInfoReturnable<MoveChoice> cir){backport$anchor=null;backport$benefits.clear();backport$health.clear();}
}
