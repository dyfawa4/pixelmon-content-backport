package local.pixelmon.backport.client;

/** Integrated MIT fractional timer: preserve sub-tick elapsed time at high frame rates. */
public final class RaidCatchClock {
    private long remainder;
    public int advance(int ticker,long millis){
        long elapsed=Math.max(0,millis),fraction=remainder+(elapsed%5)*2;
        long increment=(elapsed/5)*2+fraction/5;
        remainder=fraction%5;
        long result=(long)ticker+increment;
        if(result>Integer.MAX_VALUE){reset();return Integer.MAX_VALUE;}
        return (int)result;
    }
    public void reset(){remainder=0;}
}
