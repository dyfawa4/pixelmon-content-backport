package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.log.AttackResult;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
/** Value the remaining useful control turns without overriding legal moves or immediate KOs. */
public final class NpcSignatureTactics {
 private NpcSignatureTactics(){}
 private static boolean eligible(PixelmonWrapper own){
  if(own==null||own.bc==null||own.bc.simulateMode||own.bc.isRaid()||!own.isAlive()||!own.onBattlefield||!(own.getParticipant() instanceof TrainerParticipant))return false;
  var npc=((TrainerParticipant)own.getParticipant()).trainer;
  return !npc.getPersistentData().func_74767_n("BackportNpcDisable")&&"equal".equalsIgnoreCase(npc.getBossTier().getID());
 }
 public static double benefit(PixelmonWrapper own,Attack attack,PixelmonWrapper target){
  if(!eligible(own)||target==null||!target.isAlive()||own.isAlly(target)||attack==null||own.usingZ||attack.isZ||attack.isMax)return 0;
  if(BackportConfig.erosionEnabled()&&AshWaterBoost.eligible(own)&&attack.getMove().getAttackName().equals("Water Shuriken")){
   int turns=BackportConfig.erosionTurns();double fresh=Math.max(0,turns-Erosion.remaining(target))/(double)Math.max(1,turns);
   return 40*Math.min(1,BackportConfig.erosionReduction()/20d)*Math.min(1,turns/3d)*fresh;
  }
  if(LightningOath.signature(own,attack)){
   double fresh=Math.max(0,5-LightningOath.remaining(target))/5d;
   long partners=Arrays.stream(own.getParticipant().allPokemon).filter(p->p!=own&&p.isAlive()&&p.getMoveset().stream().anyMatch(a->a.pp>0&&a.getType()==Element.ELECTRIC&&a.getAttackCategory()!=com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS)).count();
   return (30+Math.min(16,partners*8))*fresh;
  }
  return 0;
 }
 public static void weight(PixelmonWrapper own,List<MoveChoice> choices){
  if(!eligible(own))return;
  for(var choice:choices){
   if(!choice.isAttack()||choice.tier<2||choice.targets.size()!=1||choice.attack.pp<=0||choice.attack.getDisabled()||choice.result==null||choice.result.result==AttackResult.failed||choice.result.damage<=0)continue;
   var target=choice.targets.get(0);double value=benefit(own,choice.attack,target);if(value<=0||choice.result.damage>=target.getHealth())continue;
   var threats=NpcTactics.threats(own,choice);if(NpcIntentResponse.canOutspeed(threats,own,choice.createList())&&NpcTactics.incoming(own,threats)>=own.getHealth())continue;
   double payoff=Math.min(1,2d*(target.getHealth()-choice.result.damage)/Math.max(1,target.getMaxHealth()));
   choice.weight+=(float)(value*payoff);
  }
 }
}
