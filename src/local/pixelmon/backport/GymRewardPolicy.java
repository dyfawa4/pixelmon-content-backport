package local.pixelmon.backport;
public final class GymRewardPolicy {
    public static final int LEADER_PRIZE=200000;
    private GymRewardPolicy(){}
    public static int base(boolean leader,int nativeAmount){return leader?LEADER_PRIZE:nativeAmount;}
}
