import local.pixelmon.backport.AcquisitionPolicy;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import com.google.gson.*;
import java.nio.file.*;
public final class AcquisitionVerification {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        int[][] actual={{789,790},{790,791},{790,792},{772,773},{803,804},{808,809},{891,892}};
        JsonObject catalogue=new JsonParser().parse(Files.readString(Path.of("resources/backport/advancements.json"))).getAsJsonObject();
        for(int[] route:actual){
            check(AcquisitionPolicy.evolved(route[0],route[1],false,true,true,true),"Missed real evolution "+route[1]);
            check(catalogue.getAsJsonObject("captures").has(Integer.toString(route[1])) || catalogue.getAsJsonObject("capturesByForm").has(Integer.toString(route[1])),"Missing evolved achievement "+route[1]);
            check(!AcquisitionPolicy.evolved(route[0],route[0],false,true,true,true),"Cancelled/unchanged evolution awarded");
            for(int failure=0;failure<4;failure++)check(!AcquisitionPolicy.evolved(route[0],route[1],failure==0,failure!=1,failure!=2,failure!=3),"Egg/client/other-owner/copied object awarded");
        }
        check(!AcquisitionPolicy.evolved(-1,790,false,true,true,true),"Unobserved source awarded");
        ClassNode hook=Version13StaticVerification.node("local/pixelmon/backport/mixin/EvolutionAdvancementMixin");
        boolean guarded=false,completed=false;
        for(MethodNode method:hook.methods)if(method.name.contains("afterEvolution"))for(AbstractInsnNode i:method.instructions){
            if(i instanceof FieldInsnNode && ((FieldInsnNode)i).name.equals("normalEvolution"))guarded=true;
            if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals("local/pixelmon/backport/AcquisitionAdvancements") && ((MethodInsnNode)i).name.equals("evolved"))completed=true;
        }
        check(guarded && completed,"Normal completed evolution hook not wired");
        ClassNode acquisition=Version13StaticVerification.node("local/pixelmon/backport/AcquisitionAdvancements");
        int nativeCriteria=0;boolean canonical=false,statistics=false;
        for(MethodNode method:acquisition.methods)if(!method.name.equals("raidStored"))for(AbstractInsnNode i:method.instructions){
            if(i.getOpcode()==Opcodes.IF_ACMPNE || i.getOpcode()==Opcodes.IF_ACMPEQ)canonical=true;
            if(i instanceof FieldInsnNode){String name=((FieldInsnNode)i).name;if(name.equals("LEGENDARY_CAPTURE_TRIGGER") || name.equals("SPEC_TRIGGER"))nativeCriteria++;}
            if(i instanceof MethodInsnNode && ((MethodInsnNode)i).name.equals("throwCaptureTriggers"))statistics=true;
        }
        check(canonical && nativeCriteria==2 && !statistics,"Criteria replay must use canonical object and not fake ball/catch statistics");
        ClassNode raid=Version13StaticVerification.node("local/pixelmon/backport/mixin/RaidAcquisitionMixin");
        boolean insert=false,award=false,storageFailureGuard=false;
        for(MethodNode method:raid.methods)for(AbstractInsnNode i:method.instructions){
            if(i instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)i;if(call.name.equals("addSilently"))insert=true;if(call.name.equals("raidStored")){check(insert,"Raid awarded before insertion");award=true;}}
            if(i instanceof FieldInsnNode && ((FieldInsnNode)i).owner.endsWith("TriBoolean") && ((FieldInsnNode)i).name.equals("NULL"))storageFailureGuard=true;
        }
        check(insert && award && storageFailureGuard,"Missing successful native/PC raid insertion boundary");
        System.out.println("PASS seven actual evolved legendary/UB paths, cancelled/unchanged/egg/client/owner/copy boundaries, native criteria and completed evolution wiring; no game initialized.");
    }
}
