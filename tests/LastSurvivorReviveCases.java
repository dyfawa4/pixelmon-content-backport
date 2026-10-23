package qa;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import net.minecraft.entity.player.ServerPlayerEntity;
import local.pixelmon.backport.*;
/** Focused native item execution for the last-survivor policy. */
public final class LastSurvivorReviveCases {
 static void check(boolean test,String why){if(!test)throw new AssertionError(why);}
 static Pokemon mon(int dex,String move){var p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(100);p.initialize();p.getMoveset().clear();p.getMoveset().add(new Attack(move));p.heal();return p;}
 public static void run(ServerPlayerEntity player){
  var npc=new NPCTrainer(player.field_70170_p);npc.isGymLeader=true;npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));
  for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,null);
  npc.getPokemonStorage().set(0,mon(129,"Splash"));npc.getPokemonStorage().set(1,mon(150,"Splash"));npc.getPokemonStorage().set(2,mon(445,"Earthquake"));
  var trainer=new TrainerParticipant(npc,1);var enemy=new WildPixelmonParticipant(mon(25,"Thunderbolt"));
  var battle=new BattleController(new BattleParticipant[]{trainer},new BattleParticipant[]{enemy},new BattleRules());battle.sendMessages=false;battle.battleTurn=1;
  for(var side:battle.participants){side.controlledPokemon.clear();side.controlledPokemon.add(side.allPokemon[0]);for(var p:side.allPokemon){p.bc=battle;p.setTempLevel(100);p.onBattlefield=p==side.allPokemon[0];p.setHealth(p.getMaxHealth());}}
  trainer.setBattleAI(com.pixelmonmod.pixelmon.api.battles.BattleAIMode.ADVANCED.createAI(trainer));battle.modifyStats();
  var own=trainer.allPokemon[0];var ace=trainer.allPokemon[1];var reserve=trainer.allPokemon[2];
  for(var member:trainer.allPokemon){member.pokemon.getPersistentData().func_74757_a(GymAcePolicy.MARKER,false);if(member!=own){member.onBattlefield=false;member.setHealth(0);}}own.onBattlefield=true;own.willUseItemInStack=null;
  ace.pokemon.getPersistentData().func_74757_a(GymAcePolicy.MARKER,true);ace.setHealth(0);reserve.setHealth(0);own.setHealth(1);NpcMedicine.prepare(trainer);
  var recovery=new MoveChoice(own,new Attack("Recover"),List.of(own));
  check(NpcMedicine.plan(own,recovery),"Last survivor at 1HP revives despite lethal Electric threat and planned recovery");
  check(own.willUseItemPokemon.equals(ace.getPokemonUUID()),"Marked ace preferred to higher-damage reserve");check(own.willUseItemInStack.func_77973_b()==PixelmonItems.max_revive,"Exactly native Max Revive");
  own.useItem();check(ace.isAlive()&&ace.getHealth()==ace.getMaxHealth(),"Native item revives ace to FULL HP");check(own.getHealth()==1&&own.attack==null&&own.selectedAttack==null,"Revive spends own item action, no heal or extra attack");
  own.setHealth(0);check(Arrays.stream(trainer.allPokemon).anyMatch(PixelmonWrapper::isAlive),"Surviving revived ace prevents team wipe");
  own.setHealth(1);ace.setHealth(0);battle.battleTurn++;check(NpcMedicine.plan(own,recovery)&&own.willUseItemInStack.func_77973_b()==PixelmonItems.max_potion,"Revive exhausted: emergency Max Potion instead");own.useItem();check(own.getHealth()==own.getMaxHealth(),"Emergency potion really heals");
  own.setHealth(1);battle.battleTurn++;check(NpcMedicine.plan(own,recovery)&&own.willUseItemInStack.func_77973_b()==PixelmonItems.max_potion,"Second and last emergency potion");own.useItem();own.setHealth(1);battle.battleTurn++;check(!NpcMedicine.plan(own,recovery),"No items after 1 revive and 2 heals");
  NpcMedicine.prepare(trainer);battle.battleTurn++;own.getMoveset().clear();own.getMoveset().add(new Attack("Quick Attack"));enemy.allPokemon[0].setHealth(1);
  enemy.allPokemon=Arrays.copyOf(enemy.allPokemon,2);enemy.allPokemon[1]=new PixelmonWrapper(enemy,mon(143,"Tackle"),1);enemy.allPokemon[1].bc=battle;enemy.allPokemon[1].onBattlefield=false;enemy.allPokemon[1].setTempLevel(100);enemy.allPokemon[1].setHealth(enemy.allPokemon[1].getMaxHealth());
  var finisher=trainer.getMove(own);check(NpcTactics.finishFirst(own,finisher,NpcTactics.threats(own)),"Native priority move finishes current enemy first");check(!NpcMedicine.plan(own,finisher),"No medicine when current enemy can be KO'd first, even with enemy reserve alive");
  NpcMedicine.prepare(trainer);battle.simulateMode=true;check(!NpcMedicine.plan(own,recovery),"Simulation does not queue emergency item");battle.simulateMode=false;
  npc.isGymLeader=false;NpcMedicine.prepare(trainer);check(!NpcMedicine.plan(own,recovery),"Ordinary trainer keeps old safe revive conditions");
 }
}
