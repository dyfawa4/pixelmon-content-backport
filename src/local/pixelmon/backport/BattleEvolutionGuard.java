package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;

/** Do not enqueue a visual evolution for an actor already removed by damage. */
public final class BattleEvolutionGuard {
    private BattleEvolutionGuard(){}
    public static boolean mayStart(boolean alive,boolean onField,boolean ended){return alive && onField && !ended;}
    public static boolean mayStart(PixelmonWrapper p){
        return p.bc!=null && mayStart(p.isAlive(),p.onBattlefield,p.bc.battleEnded);
    }
    public static void discardChoice(PixelmonWrapper p){
        p.willEvolve=false;p.usingZ=false;Tera.cancelSelection(p);
    }
    public static void recover(BattleController bc){
        if(bc.simulateMode || bc.battleEnded)return;
        for(BattleParticipant participant:bc.participants)if(participant instanceof TrainerParticipant){
            boolean recovered=false;
            for(PixelmonWrapper p:participant.allPokemon)if(!p.isAlive() && p.evolution!=null){
                // Native isEvolving skips clearing wait for a fainted wrapper.
                // Abort only this invalid query; retain spent resource IDs and
                // let normal faint, switching and victory logic continue.
                p.evolution.battleEvolutionConcluded=true;p.evolution.abort();p.evolution=null;
                discardChoice(p);p.evolvedThisTurn=false;p.wait=false;recovered=true;
            }
            if(recovered){
                boolean another=false;for(PixelmonWrapper p:participant.controlledPokemon)another|=p.evolution!=null && !p.evolution.isEnded();
                if(!another)participant.wait=false;
            }
        }
    }
}
