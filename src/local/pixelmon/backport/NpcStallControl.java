package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.log.AttackResult;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.status.StatusType;
/** Progress against recovery/item cycles without bypassing native immunity, legality or tactical tiers. */
public final class NpcStallControl {
    private NpcStallControl(){}
    private static boolean sustain(Attack move){return NpcMoves.recovery(move)||Set.of("Rest","Wish","Strength Sap","Leech Seed","Pain Split").contains(move.getMove().getAttackName());}
    public static int defensiveMembers(BattleController battle,int team){
        int count=0;
        for(var side:battle.participants)if(side.team!=team&&side instanceof PlayerParticipant)for(var p:side.allPokemon)if(p.isAlive()){
            long utility=p.getMoveset().stream().filter(a->a.getAttackCategory()==AttackCategory.STATUS).count();
            String ability=p.getBattleAbility().getClass().getSimpleName();
            if(utility>=2&&(p.getMoveset().stream().anyMatch(NpcStallControl::sustain)||Set.of("Regenerator","PoisonHeal").contains(ability)))count++;
        }return count;
    }
    public static boolean tauntable(PixelmonWrapper own,PixelmonWrapper foe){
        String ability=foe.getBattleAbility(own).getClass().getSimpleName();
        var held=foe.getUsableHeldItem();
        return !Set.of("MagicBounce","Oblivious","AromaVeil").contains(ability)
            &&!(held!=null&&held.getRegistryName()!=null&&held.getRegistryName().func_110623_a().equals("mental_herb")&&!AuthorHeldItems.suppressed(foe,held))
            &&!(own.getBattleAbility().getClass().getSimpleName().equals("Prankster")&&foe.hasType(com.pixelmonmod.pixelmon.api.pokemon.Element.DARK));
    }
    public static void weight(PixelmonWrapper own,List<MoveChoice> choices){
        if(!NpcTurnKnowledge.enabled(own)||own.bc.simulateMode)return;
        for(var foe:own.getOpponentPokemon())if(foe.isAlive()){
            boolean recovery=NpcTactics.knownMoves(own,foe).stream().anyMatch(NpcStallControl::sustain);
            String ability=foe.getBattleAbility(own).getClass().getSimpleName();
            for(var c:choices){
                if(c.user!=own||c.attack==null||c.attack.pp<=0||c.attack.getDisabled()||c.result==null||c.result.result==AttackResult.failed||c.tier<2||c.weight<=0||!Float.isFinite(c.weight))continue;
                String name=c.attack.getMove().getAttackName();
                if(ability.equals("Unaware")&&Set.of("Swords Dance","Nasty Plot","Tail Glow").contains(name)
                    &&!Set.of("MoldBreaker","Teravolt","Turboblaze").contains(own.getBattleAbility().getClass().getSimpleName())){
                    c.lowerTier(1);c.weight*=.25f;continue;
                }
                if(!c.targets.contains(foe))continue;
                if(ability.equals("PoisonHeal")&&Set.of("Toxic","Poison Gas","Poison Powder").contains(name)){
                    c.lowerTier(1);c.weight*=.1f;continue;
                }
                if(name.equals("Taunt")&&recovery&&tauntable(own,foe)&&foe.getStatus(StatusType.Taunt)==null)c.weight+=24;
                if(name.equals("Knock Off")&&c.result.damage>0&&foe.hasHeldItem()&&foe.isItemRemovable(own)&&!ability.equals("StickyHold")){
                    String item=foe.pokemon.getHeldItem().func_77973_b().getRegistryName().func_110623_a();
                    if(Set.of("eviolite","leftovers","black_sludge","heavy_duty_boots").contains(item))c.weight+=20;
                    // Removing an activated Toxic Orb does not remove Poison Heal's existing poison.
                }
            }
        }
    }
}
