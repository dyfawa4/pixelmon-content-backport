package local.pixelmon.backport;
/** Boundaries shared by actual acquisition hooks, independent of game initialization. */
public final class AcquisitionPolicy {
    private AcquisitionPolicy(){}
    public static boolean evolved(int before,int after,boolean egg,boolean server,boolean owned,boolean canonical){
        return before>0 && after>0 && before!=after && !egg && server && owned && canonical;
    }
}
