package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
/** Battle-clock debuff, independent of native status slots and Erosion; bench time counts. */
public final class LightningOath {
 private static final Map<PixelmonWrapper,Integer> UNTIL=new WeakHashMap<>();
 private static final Set<PixelmonWrapper> COPIES=Collections.newSetFromMap(new WeakHashMap<>());
 private LightningOath(){}
 public static double multiplier(double nativeValue){return Double.isFinite(nativeValue)?(nativeValue>1?nativeValue:0)+1.5:nativeValue;}
 public static int remaining(PixelmonWrapper p){Integer until=UNTIL.get(p);return until==null||p.bc==null?0:Math.max(0,until-p.bc.battleTurn);}
 public static boolean signature(PixelmonWrapper user,Attack attack){return user!=null&&user.getSpecies().getDex()==CapSpecies.DEX&&attack!=null&&!user.usingZ&&!attack.isZ&&"Ten Million Volts".equals(attack.getActualMove().getAttackName());}
 public static boolean affects(PixelmonWrapper user,PixelmonWrapper target,Attack attack){return user!=null&&target!=null&&attack!=null&&attack.getType()==Element.ELECTRIC&&attack.getAttackCategory()!=AttackCategory.STATUS&&(remaining(target)>0||signature(user,attack));}
 public static double effectiveness(PixelmonWrapper user,PixelmonWrapper target,Attack attack,double value){return affects(user,target,attack)?multiplier(value):value;}
 public static void apply(PixelmonWrapper user,PixelmonWrapper target){
  if(user==null||target==null||user.bc==null||user.bc!=target.bc||user.bc.simulateMode||COPIES.contains(user)||COPIES.contains(target)||!target.isAlive()||user.isAlly(target))return;
  UNTIL.put(target,user.bc.battleTurn+5);user.bc.sendToAll("pixelmon.backport.oath.start",target.getNickname());LightningOathNetwork.sync(user.bc);
 }
 public static void copy(PixelmonWrapper from,PixelmonWrapper to){COPIES.add(to);Integer value=UNTIL.get(from);if(value!=null)UNTIL.put(to,value);}
 public static void expire(BattleController battle){if(battle.simulateMode)return;for(var it=UNTIL.entrySet().iterator();it.hasNext();){var row=it.next();if(row.getKey().bc==battle&&!COPIES.contains(row.getKey())&&row.getValue()<=battle.battleTurn){it.remove();if(!battle.battleEnded)battle.sendToAll("pixelmon.backport.oath.end",row.getKey().getNickname());}}}
 public static void end(BattleController battle){if(battle.simulateMode)return;UNTIL.keySet().removeIf(p->p.bc==battle);COPIES.removeIf(p->p.bc==battle);LightningOathNetwork.sync(battle);}
}
