import local.pixelmon.backport.GymRewardPolicy;
import org.objectweb.asm.tree.*;
public final class GymPrizeVerification {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        for(int level:new int[]{1,70,100})for(int tier:new int[]{1,2,5}){
            int original=5000*level*tier;check(GymRewardPolicy.base(true,original)==200000,"Leader base scaled by level/tier");
            check(GymRewardPolicy.base(false,original)==original,"Nonleader payout changed");
        }
        ClassNode hook=Version13StaticVerification.node("local/pixelmon/backport/mixin/GymPrizeMixin");boolean identity=false,nativeBranch=false;
        for(MethodNode m:hook.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){String name=((MethodInsnNode)i).name;identity|=name.equals("isLeader");check(!name.equals("add") && !name.equals("setBalance") && !name.equals("award"),"Prize hook must not independently pay/reward again");}
        for(MethodNode m:Version13StaticVerification.node("com/pixelmonmod/pixelmon/entities/npcs/NPCTrainer").methods)if(m.name.equals("loseBattle")){
            boolean coin=false,bank=false;for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;coin|=c.name.equals("getPrizeMoneyMultiplier");bank|=c.owner.endsWith("/BankAccount") && c.name.equals("add");}nativeBranch=coin && bank;
        }
        check(identity && nativeBranch,"Native defeat/bank/item multiplier path was replaced");
        System.out.println("PASS real leader 200000 base across 1/70/100 levels and boss scaling; equal nonleaders untouched, native bank and legitimate prize-item multiplier retained, no duplicate award path; no game initialized.");
    }
}
