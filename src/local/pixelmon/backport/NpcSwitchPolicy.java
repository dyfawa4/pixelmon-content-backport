package local.pixelmon.backport;
/** Compare survival fractions, after native hazard and legal-switch evaluation. */
public final class NpcSwitchPolicy {
    private NpcSwitchPolicy(){}
    public static boolean healingBuysAction(double maximumHP,double incoming,boolean canActFirst){
        // An item spends this turn. A slower user must also survive the next hit
        // before its attacking/recovery action can repay that turn.
        return maximumHP>incoming*(canActFirst?1.15:2.05);
    }
    public static float bonus(double currentHP,double incoming,double reserveHP,double reserveIncoming,int boosts){
        if(currentHP<=0 || reserveHP<=0 || incoming<currentHP*.5 || reserveIncoming>=reserveHP*.5)return 0;
        double improvement=Math.min(2,incoming/currentHP)-reserveIncoming/reserveHP;
        if(improvement<.35)return 0;
        return (float)Math.max(0,Math.min(85,improvement*65)-Math.max(0,boosts)*8);
    }
}
