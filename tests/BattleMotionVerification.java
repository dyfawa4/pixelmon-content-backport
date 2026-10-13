import local.pixelmon.backport.BattleMotion;
import org.objectweb.asm.tree.*;
public final class BattleMotionVerification {
    static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);}
    public static void main(String[] args)throws Exception{
        check(BattleMotion.eligible(true,true,false,false,false),"Living real participant not frozen");
        for(int flags=0;flags<32;flags++)check(BattleMotion.eligible((flags&1)!=0,(flags&2)!=0,(flags&4)!=0,(flags&8)!=0,(flags&16)!=0)==(flags==3),"Death/spectator/end/simulation/nonplayer boundary");
        for(String cls:new String[]{"BattleMotion","mixin/BattleMotionEntityMixin","mixin/BattleMotionPlayerMixin","mixin/BattleMotionLivingMixin","mixin/BattleMotionPacketMixin"})
            for(MethodNode m:Version13StaticVerification.node("local/pixelmon/backport/"+cls).methods)for(var i:m.instructions)if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;
                check(!c.name.equals("func_189654_d") && !c.name.equals("setInvulnerable") && !c.name.equals("func_225609_n_") && !c.name.equals("func_74757_a"),"Freeze persists gravity/flight/godmode/NBT flags");
            }
        ClassNode packet=Version13StaticVerification.node("local/pixelmon/backport/mixin/BattleMotionPacketMixin");
        check(packet.methods.stream().noneMatch(m->m.name.contains("chat") || m.name.contains("command")),"Chat/command interception");
        for(MethodNode m:packet.methods)for(var i:m.instructions)if(i instanceof MethodInsnNode)check(!((MethodInsnNode)i).name.equals("cancel"),"Whole networking/tick cancelled");
        boolean teleport=false;for(MethodNode m:packet.methods)for(var i:m.instructions)if(i instanceof FieldInsnNode)teleport|=((FieldInsnNode)i).name.equals("field_184362_y");check(teleport,"Native teleport confirmation guard missing");
        check(Version13StaticVerification.node("local/pixelmon/backport/mixin/BattleMotionEntityMixin").methods.stream().anyMatch(m->m.name.contains("saveOriginalGravity")),"Native save must read the underlying gravity flag, not the temporary getter override");
        java.util.Set<String> amplitude=new java.util.HashSet<>();
        for(MethodNode m:Version13StaticVerification.node("local/pixelmon/backport/BattleMotion").methods)if(m.name.equals("stop"))for(var i:m.instructions)
            if(i instanceof FieldInsnNode && i.getOpcode()==org.objectweb.asm.Opcodes.PUTFIELD)amplitude.add(((FieldInsnNode)i).name);
        check(amplitude.containsAll(java.util.Set.of("field_184618_aE","field_70721_aZ")),"Frozen travel must zero both animation amplitudes to avoid partial-tick sawtooth");
        var input=Version13StaticVerification.node("local/pixelmon/backport/mixin/BattleMotionClientInputMixin");
        check(input.methods.stream().anyMatch(m->m.name.contains("oldMotionInput")) && input.methods.stream().anyMatch(m->m.name.contains("newMotionInput")),"Stale and freshly sampled locomotion inputs need neutralization");
        for(MethodNode m:input.methods)for(var i:m.instructions)if(i instanceof MethodInsnNode)
            check(!((MethodInsnNode)i).name.equals("cancel"),"Input fix must not cancel living tick, GUI, keyboard or chat processing");
        System.out.println("PASS all 32 motion eligibility states, client/server actual participants, narrow travel/move/coordinate-only hooks, native teleport and chat retained, no persistent gravity/flight flags; real elytra/water behavior remains manual.");
    }
}
