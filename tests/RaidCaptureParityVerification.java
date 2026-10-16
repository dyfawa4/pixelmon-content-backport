import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Inspect the real native RaidPlayer after Mixin application, not a copied formula. */
public final class RaidCaptureParityVerification {
    static ClassNode node(byte[] bytes){ClassNode n=new ClassNode();new ClassReader(bytes).accept(n,0);return n;}
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static int calls(MethodNode m,String name){int count=0;for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode&&((MethodInsnNode)i).name.equals(name))count++;return count;}
    public static void verify(byte[] original,byte[] patched)throws Exception{
        ClassNode before=node(original),after=node(patched);
        MethodNode nativeCatch=before.methods.stream().filter(m->m.name.equals("tryCatch")).findFirst().orElseThrow();
        MethodNode fixed=after.methods.stream().filter(m->m.name.equals("tryCatch")).findFirst().orElseThrow();
        check(calls(nativeCatch,"calculateCapture")==1&&calls(fixed,"calculateCapture")==1,"Native ball capture calculation bypassed");
        check(calls(nativeCatch,"getShakes")==1&&calls(fixed,"getShakes")==1,"Native random capture result replaced");
        check(calls(fixed,"getCaptureMethod")==1,"Selected ball capture method lost");
        int hooks=0;
        for(MethodNode method:after.methods){
            for(AbstractInsnNode i:method.instructions)if(i instanceof LdcInsnNode)check(!"BackportTeraRaidCatch".equals(((LdcInsnNode)i).cst),"Tera flag still changes catch odds");
            if(!method.name.contains("backport$forbiddenBall"))continue;
            hooks++;check(calls(method,"forbidden")==1&&calls(method,"setReturnValue")==1,"Only forbidden ball may cancel native catch");
            for(AbstractInsnNode i:method.instructions)check(i.getOpcode()!=Opcodes.ICONST_4,"Forced successful capture survived");
        }
        check(hooks==1,"Capture guard absent or duplicated");
        ClassNode preview=Version13StaticVerification.node("local/pixelmon/backport/BurstRaidCapture");
        for(MethodNode method:preview.methods)if(method.name.equals("preview")){
            check(calls(method,"outlinedBalls")==1,"Shared native capture lacks eligible Burst ball preview");
            for(AbstractInsnNode i:method.instructions)if(i instanceof LdcInsnNode)check(!"BackportTeraRaidCatch".equals(((LdcInsnNode)i).cst),"Tera capture excluded from Burst preview");
        }
        System.out.println("PASS actual transformed RaidPlayer: Tera and Max use the same native selected-ball calculateCapture/getShakes; no Tera guarantee; forbidden-ball guard and eligible Burst preview preserved.");
    }
}
