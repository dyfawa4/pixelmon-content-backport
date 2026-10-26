package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.log.AttackResult;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
/** Stop a committed support turn when native order and effects say the interruption will work. */
public final class NpcStrategicControl {
    public static void weight(PixelmonWrapper own,List<MoveChoice> choices){
        if(!NpcTurnKnowledge.present(own.bc))return;
        for(var foe:own.getOpponentPokemon()){
            var action=NpcSetupControl.submitted(foe);if(action==null||action.getAttackCategory()!=AttackCategory.STATUS)continue;
            boolean finish=choices.stream().anyMatch(c->c.isOffensiveMove()&&c.result!=null&&c.result.result!=AttackResult.failed&&c.result.damage>=foe.getHealth()&&NpcIntentResponse.first(own,c.attack,foe,action)&&AuthorForesight.succeeds(own,foe,c.attack));if(finish)continue;
            for(var choice:choices){if(choice.attack==null||choice.tier<=0||choice.result==null||choice.result.result==AttackResult.failed||!choice.targets.contains(foe)||!choice.attack.getMove().getAttackName().equals("Taunt")||!NpcIntentResponse.first(own,choice.attack,foe,action))continue;
                String ability=foe.getBattleAbility(own).getClass().getSimpleName();String held=foe.pokemon.getHeldItem().func_77973_b().getRegistryName().func_110623_a();if(ability.equals("MagicBounce")||ability.equals("Oblivious")||held.equals("mental_herb")&&!AuthorHeldItems.suppressed(foe,foe.getHeldItem()))continue;
                if(own.getBattleAbility().getClass().getSimpleName().equals("Prankster")&&foe.hasType(com.pixelmonmod.pixelmon.api.pokemon.Element.DARK))continue;
                if(!AuthorForesight.succeeds(own,foe,choice.attack))continue;
                String name=action.getMove().getAttackName();if(Set.of("Trick Room","Tailwind","Reflect","Light Screen","Aurora Veil","Stealth Rock","Spikes","Toxic Spikes","Sticky Web","Baton Pass","Shed Tail").contains(name)||NpcSetupControl.boost(action)||NpcMoves.recovery(action)){
                    choice.raiseTier(3);choice.weight=Math.max(choice.weight,180);
                }
            }
        }
    }
}
