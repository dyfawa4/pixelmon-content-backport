package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.events.battles.BattleStartedEvent;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import net.minecraft.item.Item;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
/** A trainer-battle entrance rule; ordinary party equipment and wild encounters stay unrestricted. */
public final class TrainerItemClause {
 public static boolean applies(BattleController battle){
  if(battle==null||battle.simulateMode||battle.isRaid())return false;
  for(var a:battle.participants)if(trainer(a))for(var b:battle.participants)if(a.team!=b.team&&trainer(b))return true;
  return false;
 }
 private static boolean trainer(BattleParticipant p){return p instanceof PlayerParticipant||p instanceof TrainerParticipant;}
 public static List<PixelmonWrapper> duplicate(BattleParticipant side){
  Map<Item,PixelmonWrapper> seen=new IdentityHashMap<>();
  for(var member:side.allPokemon){var item=member.pokemon.getHeldItem();if(item.func_190926_b())continue;var previous=seen.putIfAbsent(item.func_77973_b(),member);if(previous!=null)return List.of(previous,member);}
  return Collections.emptyList();
 }
 @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
 public void before(BattleStartedEvent.Pre event){
  var battle=event.getBattleController();if(!applies(battle))return;
  boolean denied=false;
  for(var side:battle.participants)if(side instanceof PlayerParticipant){
   var repeated=duplicate(side);if(repeated.isEmpty())continue;denied=true;
   var first=repeated.get(0);((PlayerParticipant)side).player.func_146105_b(new TranslationTextComponent("pixelmon.backport.item_clause.duplicate",first.pokemon.getHeldItem().func_200301_q(),first.getNickname(),repeated.get(1).getNickname()),false);
  }
  if(denied)event.setCanceled(true);
 }
 /** Runs after our final build selection, before native temporary-level scaling. */
 public static void prepare(TrainerParticipant side){
  if(!applies(side.bc))return;
  NpcHeldItems.equipUnique(Arrays.stream(side.allPokemon).map(w->w.pokemon).collect(java.util.stream.Collectors.toList()));
  for(var member:side.allPokemon){
   member.initialCopyOfPokemon.setHeldItem(member.pokemon.getHeldItem().func_77946_l());
   if(GymCities.theme(side.trainer)!=null&&!side.trainer.getPersistentData().func_74767_n("BackportNpcDisable")){
    NpcTraining.optimizeNature(member.pokemon);NpcTraining.copyNature(member.pokemon,member.initialCopyOfPokemon);
   }
  }
 }
}
