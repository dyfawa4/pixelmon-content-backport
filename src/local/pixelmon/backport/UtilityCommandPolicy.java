package local.pixelmon.backport;
/** Selectors cannot reach native expansion for an ordinary self-help command. */
public final class UtilityCommandPolicy {
    private UtilityCommandPolicy(){}
    public static boolean ownBattle(String name,String[] args){
        return name!=null && (args.length==0 || args.length==1 && name.equalsIgnoreCase(args[0]));
    }
}
