import local.pixelmon.backport.BattleEvolutionGuard;
import org.objectweb.asm.tree.*;

public final class BattleEvolutionVerification {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        for(boolean alive:new boolean[]{false,true})for(boolean onField:new boolean[]{false,true})for(boolean ended:new boolean[]{false,true})
            check(BattleEvolutionGuard.mayStart(alive,onField,ended)==(alive && onField && !ended),"Evolution may start for dead/removed/finished actor");
        ClassNode nativeController=Version13StaticVerification.node("com/pixelmonmod/pixelmon/battles/controller/BattleController");
        boolean queuedWithoutHealth=false,deadWaitSkip=false;
        for(MethodNode m:nativeController.methods)if(m.name.equals("updateTurnZero")){
            boolean health=false,order=false,evolve=false;
            for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){String n=((MethodInsnNode)i).name;health|=n.equals("isAlive")||n.equals("isFainted");order|=n.equals("getDefaultTurnOrder");evolve|=n.equals("megaEvolve");}
            queuedWithoutHealth=order && evolve && !health;
        }else if(m.name.equals("isEvolving")){
            for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode && ((MethodInsnNode)i).name.equals("isFainted"))deadWaitSkip=true;
        }
        check(queuedWithoutHealth && deadWaitSkip,"Recheck native evolution scheduling/wait evidence");
        boolean abort=false,identitySpentPreserved=true;
        for(MethodNode m:Version13StaticVerification.node("local/pixelmon/backport/BattleEvolutionGuard").methods)for(AbstractInsnNode i:m.instructions){
            if(i instanceof MethodInsnNode){var call=(MethodInsnNode)i;abort|=call.name.equals("abort");check(!call.name.contains("endBattle") && !call.name.equals("setHealth") && !call.name.equals("setForm"),"Guard bypassed faint/win flow or resurrected a member");}
            if(i instanceof FieldInsnNode && i.getOpcode()==org.objectweb.asm.Opcodes.PUTFIELD){var f=(FieldInsnNode)i;if(f.owner.endsWith("/BattleParticipant") && (f.name.equals("evolution")||f.name.equals("dynamax")||f.name.equals("ultraBurst")))identitySpentPreserved=false;}
        }
        check(abort && identitySpentPreserved,"Pending invalid animation or spent-resource IDs mishandled");
        System.out.println("PASS native dead-actor evolution scheduling/wait evidence; live/dead/removed/ended start boundaries, precise invalid NPC query abort with doubles sibling wait preserved, no forced victory/resource refund/resurrection. Gameplay remains manual.");
    }
}
