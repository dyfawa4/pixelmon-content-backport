import local.pixelmon.backport.UtilityCommandPolicy;
public final class UtilityCommandPolicyVerification {
    public static void main(String[] args){
        assert UtilityCommandPolicy.ownBattle("Alice",new String[0]);
        assert UtilityCommandPolicy.ownBattle("Alice",new String[]{"alice"});
        for(String target:new String[]{"Bob","@a","@p","@s","@e[type=player]","*"})assert !UtilityCommandPolicy.ownBattle("Alice",new String[]{target}):target;
        assert !UtilityCommandPolicy.ownBattle(null,new String[0]);
        assert !UtilityCommandPolicy.ownBattle("Alice",new String[]{"Alice","Bob"});
        System.out.println("PASS ordinary self-help target boundaries");
    }
}
