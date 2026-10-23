package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
public final class NinjaNextPriority {
    private static final Map<PixelmonWrapper,Integer> TURN=new WeakHashMap<>();
    private NinjaNextPriority(){}
    public static void grant(PixelmonWrapper p){
        if(p.bc!=null&&!p.bc.battleEnded&&NinjaTraining.allowed(p)&&p.isAlive())TURN.put(p,p.bc.battleTurn+1);
    }
    public static int bonus(PixelmonWrapper p){
        Integer turn=TURN.get(p);
        return turn!=null&&p.bc!=null&&!p.bc.battleEnded&&p.bc.battleTurn==turn&&p.isAlive()&&p.onBattlefield?1:0;
    }
    public static void copy(PixelmonWrapper from,PixelmonWrapper to){Integer turn=TURN.get(from);if(turn!=null)TURN.put(to,turn);}
    public static void clear(PixelmonWrapper p){TURN.remove(p);}
    public static void end(BattleController b){TURN.keySet().removeIf(p->p.bc==b);}
}
