package local.pixelmon.backport;
import java.time.*;
public final class NpcQuestConditions {
    private NpcQuestConditions() {}
    public static boolean date(LocalDate now, LocalDate start, LocalDate end) {
        if(now==null || start==null || end==null)return false;
        MonthDay n=MonthDay.from(now), a=MonthDay.from(start), b=MonthDay.from(end);
        return a.compareTo(b)<=0 ? n.compareTo(a)>=0 && n.compareTo(b)<=0 : n.compareTo(a)>=0 || n.compareTo(b)<=0;
    }
    public static boolean time(long ticks,long start,Long end) {
        long n=Math.floorMod(ticks,24000),a=Math.floorMod(start,24000);
        if(end==null)return n==a;
        if(Math.abs(end-start)>=24000)return true;
        long b=Math.floorMod(end,24000);
        return a<=b ? n>=a && n<=b : n>=a || n<=b;
    }
}
