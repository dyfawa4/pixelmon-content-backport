package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
/** Wild Cap Pikachu must be challenged before any ball can capture it. */
public final class CapBallGuard {
 public static final String MESSAGE="pixelmon.backport.cap.forbidden_ball";
 public static final String FIELD_MESSAGE="pixelmon.backport.cap.field_capture_blocked";
 public static boolean blocked(com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant player,PokeBall ball){
  if(player==null||player.bc==null||player.bc.simulateMode||player.bc.battleEnded||player.bc.isRaid()||TeraRaids.get(player.bc)!=null)return false;
  for(var enemy:player.getOpponents())if(enemy instanceof com.pixelmonmod.pixelmon.battles.controller.participants.WildPixelmonParticipant)
   for(var target:enemy.controlledPokemon)if(target.onBattlefield&&target.isAlive()&&target.entity!=null&&CapBallPreference.forbidden(ball,target.pokemon))return true;
  return false;
 }
 @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
 public void choose(com.pixelmonmod.pixelmon.api.events.battles.BagItemEvent.SelectItem.Pre event){
  var battle=event.getBattleController();var owner=event.getPlayer();var stack=event.getFoundItem();
  if(event.isCanceled()||battle==null||owner==null||stack==null||stack.func_190926_b())return;
  var ball=com.pixelmonmod.pixelmon.items.PokeBallPart.getPokeBall(stack);if(!ball.isPresent())return;
  var participant=battle.getParticipantForEntity(owner);
  if(!(participant instanceof com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant))return;
  var player=(com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant)participant;
  if(player.player!=owner||!blocked(player,ball.get()))return;
  event.setCanceled(true);owner.func_146105_b(new net.minecraft.util.text.TranslationTextComponent(MESSAGE),false);BossCaptureGuard.restore(player);
 }
 public static boolean fieldBlocked(PokeBall ball,PixelmonEntity target){
  return ball!=null&&target!=null&&!target.hasOwner()&&target.getTrainer()==null&&CapBallPreference.cap(target.getPokemon());
 }
}
