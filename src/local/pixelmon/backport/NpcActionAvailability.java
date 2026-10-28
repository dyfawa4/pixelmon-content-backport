package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.Thaw;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.*;
import local.pixelmon.backport.mixin.ConfusionTurnsAccess;
/** Read current native status branches without executing effects or advancing their clocks. */
public final class NpcActionAvailability {
    private NpcActionAvailability(){}
    public static boolean canAct(PixelmonWrapper p,Attack attack){
        if(p==null||attack==null||p.bc==null||!NpcTurnKnowledge.present(p.bc)||!AuthorForesight.forecast(p))return true;
        // canAttack is a cached result/temporary switch-preview flag. Native
        // takeTurn recomputes it; it is not permission for the next action.
        String move=attack.getMove().getAttackName();
        if(move.equals("Snore")||move.equals("Sleep Talk")){
            var sleep=p.getStatus(StatusType.Sleep);
            boolean asleep=sleep instanceof Sleep&&((Sleep)sleep).effectTurns>0&&!Sleep.uproarActive(p);
            if(!asleep&&!(p.getBattleAbility() instanceof com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Comatose))return false;
        }
        // Native wrapper checks persistent statuses before volatile statuses.
        for(boolean persistent:new boolean[]{true,false})for(var s:p.getStatuses()){
            if((s instanceof StatusPersist)!=persistent)continue;
            if(s instanceof Sleep){
                if(((Sleep)s).effectTurns>0&&!Sleep.uproarActive(p)
                    &&!attack.getMove().getAttackName().equals("Snore")&&!attack.getMove().getAttackName().equals("Sleep Talk"))return false;
            }else if(s instanceof Paralysis){
                if(AuthorForesight.statusChance(p,"paralysis",25))return false;
            }else if(s instanceof Freeze){
                if(attack.getMove().effects.stream().noneMatch(e->e instanceof Thaw)&&!AuthorForesight.statusChance(p,"freeze",20))return false;
            }else if(s instanceof Confusion){
                if(((ConfusionTurnsAccess)s).backport$turns()>1&&AuthorForesight.statusChance(p,"confusion",(double).33f*100))return false;
            }else if(s instanceof Flinch||s instanceof Recharge)return false;
        }
        return true;
    }
}
