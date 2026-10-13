package local.pixelmon.backport;
import java.util.Locale;
public final class LegendaryCriterionPolicy {
    private LegendaryCriterionPolicy(){}
    public static boolean accepts(String criterion,int actualDex,String form,boolean egg){
        int expected="articuno".equals(criterion.toLowerCase(Locale.ROOT))?144:"zapdos".equals(criterion.toLowerCase(Locale.ROOT))?145:"moltres".equals(criterion.toLowerCase(Locale.ROOT))?146:0;
        return expected==0 || (!egg && expected==actualDex && ("".equals(form) || "base".equals(form)));
    }
}
