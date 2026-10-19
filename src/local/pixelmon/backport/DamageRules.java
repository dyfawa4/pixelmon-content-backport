package local.pixelmon.backport;

/** Pure Gen 9 arithmetic boundary. Battle callbacks and RNG live in the adapter. */
public final class DamageRules {
    private DamageRules(){}
    public static int calculate(int level,int power,double attack,double defense,boolean spread,double weather,double critical,int roll,double stab,double effectiveness){
        if(power<=0 || effectiveness<=0 || attack<=0)return 0;
        long a=Math.max(1,(long)Math.floor(attack)),d=Math.max(1,(long)Math.floor(defense));
        long value=(((2L*level/5+2)*power*a/d)/50)+2;
        if(spread)value=modifier(value,.75);
        value=modifier(value,weather);
        value=(long)Math.floor(value*critical);
        value=value*Math.max(85,Math.min(100,roll))/100;
        value=modifier(value,stab);
        value=(long)Math.floor(value*effectiveness);
        return (int)Math.min(Integer.MAX_VALUE,Math.max(1,value));
    }
    /** Cartridge modifiers use 12-bit fixed point; exact halves round down. */
    public static long modifier(long value,double factor){long fixed=Math.round(factor*4096);return Math.max(0,(value*fixed+2047)/4096);}
    public static double itemResult(double value){return value>0?Math.max(1,Math.ceil(value-.5)):0;}
}
