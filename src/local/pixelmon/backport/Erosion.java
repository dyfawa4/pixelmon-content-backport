package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;

/** Independent volatile debuff: survives switching, expires on the battle clock, never edits stored stats. */
public final class Erosion {
    private static final class State {
        int until,reduction;
        State(int until,int reduction){this.until=until;this.reduction=reduction;}
    }
    private static final Map<PixelmonWrapper,State> STATES=new WeakHashMap<>();
    private static final Set<PixelmonWrapper> COPIES=Collections.newSetFromMap(new WeakHashMap<>());
    private Erosion(){}
    public static int remaining(PixelmonWrapper p){State s=STATES.get(p);return s==null||p.bc==null?0:Math.max(0,s.until-p.bc.battleTurn);}
    // Keep an effect applied through the end-of-turn boundary until explicit expiry,
    // so HP restoration observes the debuffed old maximum, including on the bench.
    public static boolean active(PixelmonWrapper p){return STATES.containsKey(p);}
    public static boolean isCopy(PixelmonWrapper p){return COPIES.contains(p);}
    public static void extraTurn(PixelmonWrapper p){
        State state=STATES.get(p);if(state==null||p.bc.simulateMode||COPIES.contains(p))return;
        if(--state.until<=p.bc.battleTurn){int old=p.getMaxHealth(),hp=p.getHealth();STATES.remove(p);resize(p,old,hp);if(!p.bc.battleEnded)p.bc.sendToAll("pixelmon_backport.battle.erosion_end",p.getNickname());}
    }
    public static int stat(PixelmonWrapper p,BattleStatsType stat,int original){
        State s=STATES.get(p);if(s==null)return original;
        var permanent=p.getStats();var iv=permanent.getIVs();var nature=p.getNature();
        int n=nature==null?0:stat==nature.getIncreasedStat()?1:stat==nature.getDecreasedStat()?-1:0;
        return ErosionMath.stat(p.getForm().getBattleStats().getStat(stat),s.reduction,
            iv.isHyperTrained(stat)?31:iv.getStat(stat),permanent.getEVs().getStat(stat),p.getPokemonLevelNum(),stat==BattleStatsType.HP,n);
    }
    public static void start(PixelmonWrapper user){
        if(!BackportConfig.erosionEnabled() || !AshWaterBoost.eligible(user) || user.attack==null || user.usingZ
            || !user.attack.getMove().getAttackName().equals("Water Shuriken"))return;
        // Native AI clones share real Pokemon and opponent references. They must never
        // mutate a live target's HP, network state, or debuff clock during scoring.
        if(user.bc.simulateMode || COPIES.contains(user))return;
        boolean changed=false;
        for(PixelmonWrapper target:new ArrayList<>(user.targets))
            if(target!=null && target.bc==user.bc && target.onBattlefield && target.isAlive() && !user.isAlly(target)){apply(target);changed=true;}
        if(changed){user.bc.modifyStats();user.bc.modifyStatsCancellable(user);}
    }
    public static void apply(PixelmonWrapper p){
        if(p.bc==null || p.bc.simulateMode || COPIES.contains(p))return;
        int old=p.getMaxHealth(),hp=p.getHealth();
        State s=STATES.get(p);
        if(s==null)STATES.put(p,new State(p.bc.battleTurn+BackportConfig.erosionTurns(),BackportConfig.erosionReduction()));
        else s.until=p.bc.battleTurn+BackportConfig.erosionTurns();
        resize(p,old,hp);
        p.bc.sendToAll("pixelmon_backport.battle.erosion",p.getNickname());
    }
    private static void resize(PixelmonWrapper p,int old,int hp){
        p.recalculateMaxHP();int changed=ErosionMath.ratio(hp,old,p.getMaxHealth());if(changed!=hp)p.setHealth(changed);
        if(p.onBattlefield)p.bc.modifyStats(p);
        ErosionNetwork.sync(p.bc,p);
    }
    public static void expire(BattleController b){
        if(b.simulateMode)return;
        List<PixelmonWrapper> expired=new ArrayList<>();
        for(var row:STATES.entrySet())if(row.getKey().bc==b&&!COPIES.contains(row.getKey())&&b.battleTurn>=row.getValue().until)expired.add(row.getKey());
        for(PixelmonWrapper p:expired){int old=p.getMaxHealth(),hp=p.getHealth();STATES.remove(p);resize(p,old,hp);if(!b.battleEnded)b.sendToAll("pixelmon_backport.battle.erosion_end",p.getNickname());}
    }
    public static void copy(PixelmonWrapper from,PixelmonWrapper to){COPIES.add(to);State s=STATES.get(from);if(s!=null)STATES.put(to,new State(s.until,s.reduction));}
    public static void end(BattleController b){
        if(b.simulateMode)return;
        for(PixelmonWrapper p:new ArrayList<>(STATES.keySet()))if(p.bc==b&&!COPIES.contains(p)){
            int old=p.getMaxHealth(),hp=p.getHealth();STATES.remove(p);p.recalculateMaxHP();int changed=ErosionMath.ratio(hp,old,p.getMaxHealth());if(changed!=hp)p.setHealth(changed);ErosionNetwork.sync(b,p);
        }
        STATES.keySet().removeIf(p->p.bc==b);COPIES.removeIf(p->p.bc==b);
    }
}
