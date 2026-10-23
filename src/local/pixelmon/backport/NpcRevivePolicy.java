package local.pixelmon.backport;
/** Spend the single revive on a safe, valuable reinforcement, rather than the first fainted slot. */
public final class NpcRevivePolicy {
    private NpcRevivePolicy(){}
    public static boolean worthwhile(double health,double incoming,int alive,double candidate,double reserve){
        if(health<=0||candidate<=0||incoming>=health*.55)return false;
        if(alive<=1)return true;
        return candidate>=reserve*(alive>=3?1.15:.85);
    }
}
