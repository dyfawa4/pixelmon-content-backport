package local.pixelmon.backport;
/** Exact dex metadata prevents an empty/unresolved native specification from matching other species. */
public final class SpeciesCriterionPolicy {
    private SpeciesCriterionPolicy(){}
    public static boolean accepts(int expected,int actual,boolean egg){return expected<=0 || actual==expected && !egg;}
}
