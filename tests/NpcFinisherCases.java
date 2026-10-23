package qa;
import java.util.*;
import java.lang.reflect.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import net.minecraft.entity.player.ServerPlayerEntity;
import local.pixelmon.backport.*;
/** Focused checks through the transformed native AI picker, with native damage simulation. */
public final class NpcFinisherCases {
 static void check(boolean test,String why){if(!test)throw new AssertionError(why);}
 static Pokemon mon(int dex,String...moves){var p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(100);p.initialize();p.getMoveset().clear();for(var m:moves)p.getMoveset().add(new Attack(m));p.heal();return p;}
 public static void run(ServerPlayerEntity player)throws Exception{
  var npc=new NPCTrainer(player.field_70170_p);npc.isGymLeader=true;npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,null);npc.getPokemonStorage().set(0,mon(472,"Earthquake","Fire Fang","Toxic","Roost"));
  var trainer=new TrainerParticipant(npc,1);var enemy=new WildPixelmonParticipant(mon(CapSpecies.DEX,"Splash"));
  var b=new BattleController(new BattleParticipant[]{trainer},new BattleParticipant[]{enemy},new BattleRules());b.sendMessages=false;b.battleTurn=1;
  for(var side:b.participants){side.controlledPokemon.clear();side.controlledPokemon.add(side.allPokemon[0]);for(var p:side.allPokemon){p.bc=b;p.setTempLevel(100);p.onBattlefield=p==side.allPokemon[0];p.setHealth(p.getMaxHealth());}}
  trainer.setBattleAI(com.pixelmonmod.pixelmon.api.battles.BattleAIMode.ADVANCED.createAI(trainer));b.modifyStats();var own=trainer.allPokemon[0];var target=enemy.allPokemon[0];for(var p:trainer.allPokemon)if(p!=own){p.onBattlefield=false;p.setHealth(0);}own.onBattlefield=true;own.choiceLocked=null;own.pokemon.setHeldItem(net.minecraft.item.ItemStack.field_190927_a);own.getMoveset().clear();for(var m:List.of("Earthquake","Fire Fang","Toxic","Roost"))own.getMoveset().add(new Attack(m));own.getBattleStats().modifyStat(6,com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.SPEED);target.setHealth(30);
  var contextClass=Class.forName("local.pixelmon.backport.NpcTurnKnowledge$Context");var ctor=contextClass.getDeclaredConstructor(BattleController.class);ctor.setAccessible(true);var current=NpcTurnKnowledge.class.getDeclaredField("CURRENT");current.setAccessible(true);@SuppressWarnings("unchecked")var scope=(ThreadLocal<Object>)current.get(null);var old=scope.get();scope.set(ctor.newInstance(b));
  var weighted=TacticalAI.class.getDeclaredMethod("getWeightedAttackChoices",PixelmonWrapper.class);weighted.setAccessible(true);var picker=TacticalAI.class.getDeclaredMethod("pickBestChoice",List.class,List.class);picker.setAccessible(true);
  try{
   @SuppressWarnings("unchecked")var pool=(List<MoveChoice>)weighted.invoke(own.getBattleAI(),own);
   var eq=pool.stream().filter(c->c.attack.getMove().getAttackName().equals("Earthquake")).findFirst().orElseThrow();
   check(eq.result.damage==target.getHealth()&&eq.result.fullDamage*.9>target.getHealth(),"Native KO damage is capped but raw overkill exceeds target: "+eq.result.damage+"/"+eq.result.fullDamage+" hp="+target.getHealth());
   check(NpcTactics.finishFirst(own,eq,NpcTactics.threats(own)),"Scoped overkill remains a first-strike KO");
   for(var c:pool)if(c!=eq){c.tier=3;c.weight=1000000;}
   var selected=(MoveChoice)picker.invoke(own.getBattleAI(),pool,pool);check(selected==eq,"Real native picker prefers Earthquake KO over inflated status/secondary effects");
   var actual=trainer.getMove(own);check(actual.attack.getMove().getAttackName().equals("Earthquake"),"Native AdvancedAI actually selects Earthquake");
   target.setHealth(1);@SuppressWarnings("unchecked")var tiny=(List<MoveChoice>)weighted.invoke(own.getBattleAI(),own);for(var c:tiny)if(c.attack.getMove().getAttackName().equals("Fire Fang")){c.weight=1000000;c.tier=3;}
   selected=(MoveChoice)picker.invoke(own.getBattleAI(),tiny,tiny);check(selected.attack.getMove().getAttackName().equals("Earthquake"),"Both lethal:100 accuracy preferred over95 despite effect score");
   eq.result.result=com.pixelmonmod.pixelmon.battles.controller.log.AttackResult.failed;check(!NpcTactics.finishFirst(own,eq,List.of()),"Failed/protected hit is not a finisher");eq.result.result=com.pixelmonmod.pixelmon.battles.controller.log.AttackResult.hit;eq.result.damage=0;check(!NpcTactics.finishFirst(own,eq,List.of()),"Immunity/zero damage not a finisher");
   target.setHealth(target.getMaxHealth());eq.result.damage=1;eq.tier=3;check(!NpcTactics.finishFirst(own,eq,List.of()),"Effect-raised tier3 cannot masquerade as KO");
   System.out.println("NPC_FINISHER_PASS native capped/raw damage; active scope; real picker and AdvancedAI Earthquake; high Toxic/FireFang score; accuracy; blocked, immune and non-KO guards");
  }finally{if(old==null)scope.remove();else scope.set(old);}
 }
}
