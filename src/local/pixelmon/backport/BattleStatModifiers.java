package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
/** Native stat integration only. Each effect owns its state, formula, clock, cure and packets. */
public final class BattleStatModifiers {
 public static final BattleStatsType[] STATS={BattleStatsType.HP,BattleStatsType.ATTACK,BattleStatsType.DEFENSE,BattleStatsType.SPECIAL_ATTACK,BattleStatsType.SPECIAL_DEFENSE,BattleStatsType.SPEED};
 public static int stat(PixelmonWrapper p,BattleStatsType type,int original){
  boolean erosion=Erosion.active(p),calamity=CalamityBattle.active(p);
  if(!erosion)return calamity?CalamityBattle.stat(p,type,original):original;
  if(!calamity)return Erosion.stat(p,type,original);
  // Both reductions act on the species base before the native IV/EV/nature rounding.
  // Applying already-rounded displayed stats twice would change the old stack result.
  int base=p.getForm().getBattleStats().getStat(type),reduced=Math.max(1,base-Erosion.reduction(p)-CalamityBattle.reduction(p));boolean hp=type==BattleStatsType.HP;
  if(hp&&base==1)return 1;
  var stats=p.getStats();var iv=stats.getIVs();int ivValue=iv.isHyperTrained(type)?31:iv.getStat(type);
  float value=(ivValue+2f*reduced+stats.getEVs().getStat(type)/4f+(hp?100:0))*p.getPokemonLevelNum()/100f+(hp?10:5);
  if(hp)return Math.max(1,(int)value);
  var nature=p.getNature();int n=nature==null?0:type==nature.getIncreasedStat()?1:type==nature.getDecreasedStat()?-1:0;
  return Math.max(1,(int)((float)Math.floor(value)*(n>0?1.1f:n<0?.9f:1f)));
 }
}
