package local.pixelmon.backport;

/** Pure per-battle budget; selecting/rejecting an item never changes this state. */
public final class GymItemAllowance {
    public static final int TOTAL=3,REVIVALS=1,RESTORES=2;
    public enum Denial {NONE,TOTAL,REVIVAL,RESTORE}
    private int used,revived;
    public Denial denial(boolean revival,int pending,int pendingRevivals){
        if(used+pending>=TOTAL)return Denial.TOTAL;
        if(revival && revived+pendingRevivals>=REVIVALS)return Denial.REVIVAL;
        if(!revival && used-revived+pending-pendingRevivals>=RESTORES)return Denial.RESTORE;
        return Denial.NONE;
    }
    public void successfulUse(boolean revival){used++;if(revival)revived++;}
    public int used(){return used;}
    public int remaining(){return Math.max(0,TOTAL-used);}
    public int remainingRevivals(){return Math.max(0,REVIVALS-revived);}
    public int remainingRestores(){return Math.max(0,RESTORES-used+revived);}
}
