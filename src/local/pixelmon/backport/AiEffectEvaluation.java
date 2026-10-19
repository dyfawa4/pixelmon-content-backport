package local.pixelmon.backport;

import java.util.List;
import com.pixelmonmod.pixelmon.battles.attacks.EffectBase;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;

/** Scope temporary native stat simulations, including their simulateMode=false sections. */
public final class AiEffectEvaluation {
    private AiEffectEvaluation(){}
    private static final ThreadLocal<Boolean> ACTIVE=new ThreadLocal<>();
    public static boolean active(){return Boolean.TRUE.equals(ACTIVE.get());}
    public static void weight(EffectBase effect,PixelmonWrapper user,MoveChoice choice,List<MoveChoice> choices,
            List<MoveChoice> bestChoices,List<MoveChoice> opponents,List<MoveChoice> bestOpponents){
        Boolean previous=ACTIVE.get();var battle=user.bc;
        boolean simulation=battle.simulateMode,messages=battle.sendMessages,stats=battle.simulateStats;
        ACTIVE.set(true);
        try{effect.weightEffect(user,choice,choices,bestChoices,opponents,bestOpponents);}
        finally{
            battle.simulateMode=simulation;battle.sendMessages=messages;battle.simulateStats=stats;
            if(previous==null)ACTIVE.remove();else ACTIVE.set(previous);
        }
    }
}
