package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
/** Permanent battle binding for Calamity. The stored curse survives switch and battle end. */
public final class CalamityBattle {
 private static final Map<PixelmonWrapper,Integer> STATES=new WeakHashMap<>();
 private static final Set<PixelmonWrapper> COPIES=Collections.newSetFromMap(new WeakHashMap<>());
 public static boolean active(PixelmonWrapper p){return STATES.containsKey(p);}
 public static boolean isCopy(PixelmonWrapper p){return COPIES.contains(p);}
 public static int reduction(PixelmonWrapper p){return STATES.getOrDefault(p,0);}
 public static void persistent(PixelmonWrapper p,int old,int hp,int reduction){if(p.bc==null||p.bc.simulateMode||COPIES.contains(p))return;STATES.put(p,reduction);resize(p,old,hp);}
 public static int stat(PixelmonWrapper p,BattleStatsType type,int original){Integer reduction=STATES.get(p);if(reduction==null)return original;var stats=p.getStats();var nature=p.getNature();int n=nature==null?0:type==nature.getIncreasedStat()?1:type==nature.getDecreasedStat()?-1:0;return CalamityMath.stat(p.getForm().getBattleStats().getStat(type),reduction,stats.getIVs().isHyperTrained(type)?31:stats.getIVs().getStat(type),stats.getEVs().getStat(type),p.getPokemonLevelNum(),type==BattleStatsType.HP,n);}
 private static void resize(PixelmonWrapper p,int old,int hp){p.recalculateMaxHP();int changed=CalamityMath.ratio(hp,old,p.getMaxHealth());if(changed!=hp)p.setHealth(changed);if(p.onBattlefield)p.bc.modifyStats(p);p.update(com.pixelmonmod.pixelmon.comm.EnumUpdateType.HP);CalamityNetwork.sync(p.bc,p);}
 public static void cured(com.pixelmonmod.pixelmon.api.pokemon.Pokemon pokemon){for(var p:new ArrayList<>(STATES.keySet()))if(p.pokemon==pokemon&&!COPIES.contains(p)&&p.bc!=null&&!p.bc.simulateMode){int old=p.getMaxHealth(),hp=p.getHealth();STATES.remove(p);resize(p,old,hp);}}
 public static void copy(PixelmonWrapper from,PixelmonWrapper to){COPIES.add(to);Integer reduction=STATES.get(from);if(reduction!=null)STATES.put(to,reduction);}
 public static void end(BattleController b){if(b.simulateMode)return;for(var p:new ArrayList<>(STATES.keySet()))if(p.bc==b&&!COPIES.contains(p)){int old=p.getMaxHealth(),hp=p.getHealth();STATES.remove(p);p.recalculateMaxHP();int changed=CalamityMath.ratio(hp,old,p.getMaxHealth());if(changed!=hp)p.setHealth(changed);p.update(com.pixelmonmod.pixelmon.comm.EnumUpdateType.HP);CalamityNetwork.sync(b,p);}STATES.keySet().removeIf(p->p.bc==b);COPIES.removeIf(p->p.bc==b);}
}
