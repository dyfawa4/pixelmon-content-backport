package local.pixelmon.backport.mixin;
import java.util.*;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import local.pixelmon.backport.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extend native switch simulation; never generate a new switch or read a hidden enemy moveset. */
@Mixin(value=AdvancedAI.class,remap=false)
public abstract class NpcAdvancedMixin {
    @Unique private PixelmonWrapper backport$anchor;
    @Unique private double backport$before;
    @Unique private final Map<UUID,Float> backport$benefits=new HashMap<>();
    @Inject(method="getNextMove",at=@At("HEAD"))
    private void backport$start(PixelmonWrapper own,CallbackInfoReturnable<MoveChoice> cir){
        backport$anchor=null;backport$benefits.clear();backport$before=0;
        if(own!=null && own.bc!=null && !own.bc.simulateMode && !own.bc.isRaid()
            && own.getParticipant() instanceof TrainerParticipant && NpcGimmicks.enabled(((TrainerParticipant)own.getParticipant()).trainer))backport$anchor=own;
    }
    @Inject(method="getNextOpponentMoves",at=@At("RETURN"))
    private void backport$survival(PixelmonWrapper target,List<PixelmonWrapper> opponents,CallbackInfoReturnable<List<MoveChoice>> cir){
        PixelmonWrapper own=backport$anchor;if(own==null || !own.bc.simulateMode)return;
        double incoming=NpcTactics.incoming(target,cir.getReturnValue());
        if(target==own)backport$before=incoming;
        else backport$benefits.put(target.getPokemonUUID(),NpcSwitchPolicy.bonus(own.getHealth(),backport$before,target.getHealth(),incoming,own.getBattleStats().getSumIncreases()));
    }
    @Redirect(method="getNextMove",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/controller/ai/MoveChoice;raiseWeight(F)V"))
    private void backport$rewardSafeSwitch(MoveChoice choice,float weight){
        Float benefit=backport$benefits.remove(choice.switchPokemon);
        choice.raiseWeight(weight+(benefit==null?0:benefit));
    }
    @Inject(method="getNextMove",at=@At("RETURN"))
    private void backport$end(PixelmonWrapper own,CallbackInfoReturnable<MoveChoice> cir){backport$anchor=null;backport$benefits.clear();}
}
