package local.pixelmon.backport;
public final class FieldSequencePolicy {
    private FieldSequencePolicy(){}
    public static boolean completeStep(int dex,boolean construct,String form){return dex==718 && construct && !"complete".equals(form) && form!=null && !form.startsWith("mega");}
    public static boolean chain(boolean active,boolean construct,String current,String target,boolean stone){return active && construct && "complete".equals(current) && "complete".equals(target) && stone;}
}
