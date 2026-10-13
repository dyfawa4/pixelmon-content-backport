import com.google.gson.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.*;

public final class AcquisitionCoverageVerification {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        ClassNode acquisition=Version13StaticVerification.node("local/pixelmon/backport/AcquisitionAdvancements");
        Set<String> actualStorage=new HashSet<>();int identity=0;
        for(MethodNode method:acquisition.methods)if(method.name.equals("ownedStored"))for(AbstractInsnNode instruction:method.instructions){
            if(instruction instanceof MethodInsnNode){var call=(MethodInsnNode)instruction;if(call.owner.endsWith("/StorageProxy"))actualStorage.add(call.name);}
            if(instruction.getOpcode()==Opcodes.IF_ACMPEQ || instruction.getOpcode()==Opcodes.IF_ACMPNE)identity++;
        }
        check(actualStorage.contains("getParty") && actualStorage.contains("getPCForPlayer") && identity>=3,"Client/NPC storage copies must fail canonical server storage identity");
        ClassNode queue=Version13StaticVerification.node("local/pixelmon/backport/AcquisitionQueue");boolean owner=false,canonical=false,limit=false,scanPC=false,scanParty=false;
        for(MethodNode method:queue.methods)for(AbstractInsnNode instruction:method.instructions){
            if(instruction instanceof IntInsnNode && ((IntInsnNode)instruction).operand==128)limit=true;
            if(instruction instanceof MethodInsnNode){var call=(MethodInsnNode)instruction;
                owner|=call.name.equals("getOwnerPlayer");canonical|=call.name.equals("ownedStored");scanPC|=call.name.equals("getPCForPlayer");scanParty|=call.name.equals("getParty");
                check(!call.name.equals("throwCaptureTriggers") && !call.name.equals("incrementStat"),"General acquisition must not fake a capture/ball statistic");
            }
        }
        check(owner && canonical && limit && scanPC && scanParty,"Missing bounded canonical acquisition/login coverage");
        ClassNode hook=Version13StaticVerification.node("local/pixelmon/backport/mixin/FieldMegaPokemonMixin");Set<String> hooks=new HashSet<>();
        for(MethodNode method:hook.methods)if(method.name.contains("acquired") || method.name.contains("speciesAcquired") || method.name.contains("hatched")){
            boolean queued=false;for(AbstractInsnNode i:method.instructions)if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.endsWith("/AcquisitionQueue") && ((MethodInsnNode)i).name.equals("stored"))queued=true;
            check(queued,"Acquisition hook directly awards before insert");hooks.add(method.name);
        }
        check(hooks.size()==3,"First insertion, actual species change and egg-state coverage missing");
        check(queue.methods.stream().anyMatch(m->m.name.equals("hatched")) && queue.methods.stream().anyMatch(m->m.name.equals("received")) && queue.methods.stream().anyMatch(m->m.name.equals("login")) && queue.methods.stream().anyMatch(m->m.name.equals("stopped")),"Native completed hatch/receive/login/lifecycle coverage missing");
        for(String cls:new String[]{"LegendAdvancements","AppearanceAdvancements"})for(MethodNode method:Version13StaticVerification.node("local/pixelmon/backport/"+cls).methods)if(method.name.equals("captured") || method.name.equals("capture")){
            if(method.desc.contains("CaptureEvent$SuccessfulCapture"))for(AbstractInsnNode i:method.instructions)if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;check(!c.name.equals("trigger") && !c.name.equals("award"),"SuccessfulCapture pre-insert must not award raw disguises");}
        }
        for(int dex:new int[]{773,790,791,792,804,809,892,1026}){
            JsonObject adv=Version13StaticVerification.resource("data/pixelmon_backport/advancements/legends/capture_"+dex+".json");
            String description=adv.getAsJsonObject("display").getAsJsonObject("description").get("text").getAsString();
            check(description.contains("获得") && !description.contains("捕捉"),"Obtained-species description still capture-only: "+dex);
        }
        System.out.println("PASS real first insertion/species/hatch/receive and existing party+PC login coverage, server storage identity against copies, bounded deferred work/no forged capture stats, retired pre-insert awards and obtained wording; no game initialized.");
    }
}
