package local.pixelmon.backport;

/** Eligibility, not a promise of damage or capture success against a chosen target. */
public final class BurstDisplayPolicy {
    public static final int ELECTRIC_ATTACKER=1, CAPTURE=2, CAP_TARGET=4, GROUNDED=8, FIELD=16;
    private BurstDisplayPolicy(){}
    public static boolean move(int flags,String name,boolean electricMove,boolean damaging){
        if((flags&FIELD)==0)return false;
        // These native effects read the terrain itself, regardless of the user's species.
        if("Nature Power".equals(name)||"Camouflage".equals(name)||"Secret Power".equals(name)||"Rising Voltage".equals(name))return true;
        if("Terrain Pulse".equals(name))return (flags&GROUNDED)!=0;
        return damaging && electricMove && (flags&(ELECTRIC_ATTACKER|GROUNDED))!=0;
    }
    public static boolean ball(double nativeBonus,int nativeRate,boolean forbidden,boolean guaranteed){
        return !forbidden && !guaranteed && nativeRate>0 && Double.isFinite(nativeBonus) && nativeBonus>0;
    }
}
