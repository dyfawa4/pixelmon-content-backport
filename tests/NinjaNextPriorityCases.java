package qa;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import local.pixelmon.backport.*;
public final class NinjaNextPriorityCases {
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 public static void run(ServerPlayerEntity player){
  var p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(658));p.setForm("ash");p.setLevelNum(100);p.initialize();p.heal();
  var own=new WildPixelmonParticipant(p);var other=new WildPixelmonParticipant(PokemonFactory.create(PixelmonSpecies.fromNationalDex(129)));
  var b=new BattleController(new BattleParticipant[]{own},new BattleParticipant[]{other},new com.pixelmonmod.pixelmon.battles.api.rules.BattleRules());b.sendMessages=false;b.battleTurn=10;
  var w=own.allPokemon[0];w.bc=b;w.onBattlefield=true;w.setHealth(w.getMaxHealth());w.attack=new Attack("Ninja Training");
  var effect=new BackportEffects.NinjaTraining();check(effect.applyEffectDuring(w,w)==com.pixelmonmod.pixelmon.battles.controller.log.AttackResult.proceed,"Training successfully applies");
  check(NinjaNextPriority.bonus(w)==0,"No priority on the setup turn");
  b.battleTurn++;w.attack=new Attack("Water Shuriken");check(BattlePriorityHelper.calculatePriority(w)==2,"Native priority1 Water Shuriken becomes2 next turn");
  var copy=new PixelmonWrapper(w);check(NinjaNextPriority.bonus(copy)==1,"AI simulation copy retains next-turn priority");
  w.attack=new Attack("Surf");check(BattlePriorityHelper.calculatePriority(w)==1,"Normal priority0 move becomes1");
  NinjaNextPriority.grant(w);check(NinjaNextPriority.bonus(w)==0,"Refresh schedules next turn, never stacks");
  b.battleTurn++;check(NinjaNextPriority.bonus(w)==1,"Refreshed next turn");w.resetOnSwitch();check(NinjaNextPriority.bonus(w)==0,"Switch clears bonus");
  NinjaNextPriority.grant(w);b.battleTurn+=2;check(NinjaNextPriority.bonus(w)==0,"Following turn expires");NinjaNextPriority.end(b);b.battleEnded=true;
  check(GreninjaEnhancement.trainingDescription("zh_cn").contains("优先度提高1级"),"Actual dynamic tooltip includes priority");
  System.out.println("NINJA_PRIORITY_PASS actual effect, native priority helper and AI copy; setup/next/following turns, refresh, switch cleanup, dynamic tooltip");
 }
}
