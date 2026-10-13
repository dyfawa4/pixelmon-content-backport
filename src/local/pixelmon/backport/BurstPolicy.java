package local.pixelmon.backport;

/** Native type effectiveness enters unchanged; neutral hits and immunities stay so. */
public final class BurstPolicy {
    private BurstPolicy(){}
    public static double effectiveness(double nativeMultiplier,boolean ownAttacker,boolean electricAttacker,
                                       boolean electricMove,boolean ownDefender,boolean electricDefender){
        if(!Double.isFinite(nativeMultiplier) || nativeMultiplier<=0 || nativeMultiplier==1)return nativeMultiplier;
        if(ownAttacker && electricAttacker && electricMove)return nativeMultiplier*1.25;
        if(!ownAttacker && ownDefender && electricDefender)return nativeMultiplier*.75;
        return nativeMultiplier;
    }
    public static double catchBonus(double nativeBonus,boolean enabled,boolean forbidden){
        if(forbidden)return 0;
        return enabled && nativeBonus>0 && Double.isFinite(nativeBonus)?nativeBonus+1:nativeBonus;
    }
    public static double spawnChance(double configured){return Math.max(0,Math.min(1,configured)-.4);}
    // Native legendary attempts have already passed their configured chance.
    // P * retention(P) = max(0, P - .4); do not apply another P or multiply by .6.
    public static double spawnRetention(double configured){return configured>0?spawnChance(configured)/Math.min(1,configured):0;}
}
