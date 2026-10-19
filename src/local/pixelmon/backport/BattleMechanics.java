package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.battles.attacks.*;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.*;

/** Shared semantic boundaries; effects never infer their source from a stale selected move. */
public final class BattleMechanics {
    private BattleMechanics(){}
    public interface TargetSecondary {}
    private static final class EffectContext {
        final PixelmonWrapper source,target;
        EffectContext(PixelmonWrapper source,PixelmonWrapper target){this.source=source;this.target=target;}
    }
    private static final ThreadLocal<EffectContext> EFFECT=new ThreadLocal<>();
    private static final Map<PixelmonWrapper,Attack> CONNECTED=new WeakHashMap<>();
    public static void apply(EffectBase effect,PixelmonWrapper source,PixelmonWrapper target){
        if(ReferenceItems.blocksSecondary(effect,source,target)){effect.changeChance(1f);return;}
        EffectContext previous=EFFECT.get();EFFECT.set(new EffectContext(source,target));
        try{effect.applyEffect(source,target);}finally{if(previous==null)EFFECT.remove();else EFFECT.set(previous);}
    }
    public static void statusAdded(StatusBase status,PixelmonWrapper target,PixelmonWrapper source,boolean added){
        EffectContext context=EFFECT.get();
        if(!added || source==null || source==target || context==null || context.source!=source || context.target!=target
            || !target.getStatuses().contains(status) || source.getSpecies().getDex()!=1025
            || !(source.getBattleAbility() instanceof BackportAbilities.PoisonPuppeteer)
            || (status.type!=StatusType.Poison && status.type!=StatusType.PoisonBadly))return;
        Confusion.confuse(source,target);
    }
    public static AttackCategory adaptiveCategory(PixelmonWrapper user){
        return staged(Erosion.stat(user,BattleStatsType.ATTACK,user.getStats().getAttack()),user.getBattleStats().getStage(BattleStatsType.ATTACK))
            >staged(Erosion.stat(user,BattleStatsType.SPECIAL_ATTACK,user.getStats().getSpecialAttack()),user.getBattleStats().getStage(BattleStatsType.SPECIAL_ATTACK))
            ?AttackCategory.PHYSICAL:AttackCategory.SPECIAL;
    }
    public static int staged(int stat,int stage){stage=Math.max(-6,Math.min(6,stage));return Math.max(1,stage>=0?stat*(2+stage)/2:stat*2/(2-stage));}
    public static int changedMaxHP(int hp,int oldMax,int newMax){return hp<=0?0:Math.max(1,Math.min(newMax,newMax-(oldMax-hp)));}
    public static void beginMove(PixelmonWrapper user){CONNECTED.remove(user);}
    public static void connected(PixelmonWrapper user){CONNECTED.put(user,user.attack);}
    public static boolean consumeConnection(PixelmonWrapper user){return CONNECTED.remove(user)==user.attack && user.attack!=null;}
    public static void end(BattleController battle){CONNECTED.keySet().removeIf(p->p.bc==battle);}
}
