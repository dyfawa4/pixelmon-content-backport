package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
/** Persist the Drowned curse on the Pokemon; the Greninja clock remains a separate volatile state. */
public final class DrownedErosion {
 public static final String TAG="BackportDrownedErosion";
 public static final BattleStatsType[] STATS={BattleStatsType.HP,BattleStatsType.ATTACK,BattleStatsType.DEFENSE,BattleStatsType.SPECIAL_ATTACK,BattleStatsType.SPECIAL_DEFENSE,BattleStatsType.SPEED};
 private static final java.util.Map<java.util.UUID,ClientStats> CLIENT=new java.util.concurrent.ConcurrentHashMap<>();
 private static final class ClientStats {final String form;final int level;final int[] values;ClientStats(String f,int l,int[] v){form=f;level=l;values=v.clone();}}
 public static void client(java.util.UUID id,String form,int level,int[] values){if(values==null)CLIENT.remove(id);else CLIENT.put(id,new ClientStats(form,level,values));}
 public static void clearClient(){CLIENT.clear();}
 public static boolean active(Pokemon pokemon){return pokemon.getPersistentData().func_74762_e(TAG)>0;}
 public static int stat(Pokemon pokemon,BattleStatsType stat,int original){
  if(!active(pokemon)||stat==BattleStatsType.ACCURACY||stat==BattleStatsType.EVASION)return original;
  // Native party packets contain calculated stats, not necessarily the private
  // IV/EV inputs. Recomputing them on the client produces a different maximum.
  if(net.minecraftforge.fml.common.thread.EffectiveSide.get()==net.minecraftforge.fml.LogicalSide.CLIENT){var row=CLIENT.get(pokemon.getUUID());int index=java.util.Arrays.asList(STATS).indexOf(stat);if(row!=null)return index>=0&&row.form.equals(pokemon.getForm().getName())&&row.level==pokemon.getPokemonLevel()?row.values[index]:original;}
  var stats=pokemon.getStats();var nature=pokemon.getNature();int n=nature==null?0:stat==nature.getIncreasedStat()?1:stat==nature.getDecreasedStat()?-1:0;
  return ErosionMath.stat(pokemon.getForm().getBattleStats().getStat(stat),pokemon.getPersistentData().func_74762_e(TAG),stats.getIVs().isHyperTrained(stat)?31:stats.getIVs().getStat(stat),stats.getEVs().getStat(stat),pokemon.getPokemonLevel(),stat==BattleStatsType.HP,n);
 }
 public static void enter(PixelmonWrapper pokemon){
  if(pokemon.bc==null||pokemon.bc.simulateMode||Erosion.isCopy(pokemon)||!(pokemon.getParticipant() instanceof PlayerParticipant)||Erosion.permanent(pokemon))return;
  boolean cursed=active(pokemon.pokemon);
  if(GymChallenge.author(pokemon.bc))cursed=true;
  if(!cursed)for(var opponent:pokemon.getParticipant().getOpponents())if(opponent instanceof WildPixelmonParticipant)for(var target:opponent.controlledPokemon)if(target.entity!=null&&DrownedBosses.is(target.entity))cursed=true;
  if(!cursed)return;int old=pokemon.getMaxHealth(),hp=pokemon.getHealth();
  if(!active(pokemon.pokemon)){pokemon.pokemon.getPersistentData().func_74768_a(TAG,BackportConfig.erosionReduction());pokemon.bc.sendToAll("pixelmon_backport.battle.drowned_erosion",pokemon.getNickname());}
  Erosion.persistent(pokemon,old,hp,pokemon.pokemon.getPersistentData().func_74762_e(TAG));
 }
 @SubscribeEvent public void start(com.pixelmonmod.pixelmon.api.events.battles.BattleStartedEvent.Post event){var battle=event.getBattleController();if(battle!=null&&!battle.simulateMode)for(var participant:battle.participants)for(var pokemon:participant.allPokemon)if(pokemon.onBattlefield)enter(pokemon);}
 public static void cured(Pokemon pokemon){pokemon.getPersistentData().func_82580_o(TAG);Erosion.cured(pokemon);}
}
