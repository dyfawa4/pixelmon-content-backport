package local.pixelmon.backport;

/** Battle-only species base reduction. Integer boundaries match the native stat calculator. */
public final class ErosionMath {
    private ErosionMath(){}
    public static int stat(int base,int reduction,int iv,int ev,int level,boolean hp,int nature){
        int reduced=Math.max(1,base-reduction);
        // The native fixed-HP exception is a property of an original base HP of 1.
        if(hp && base==1)return 1;
        float value=(iv+2f*reduced+ev/4f+(hp?100:0))*level/100f+(hp?10:5);
        if(hp)return Math.max(1,(int)value);
        value=(float)Math.floor(value);
        return Math.max(1,(int)(value*(nature>0?1.1f:nature<0?.9f:1f)));
    }
    public static int ratio(int hp,int oldMax,int newMax){
        return hp<=0?0:Math.max(1,Math.min(newMax,(int)((long)hp*newMax/Math.max(1,oldMax))));
    }
}
