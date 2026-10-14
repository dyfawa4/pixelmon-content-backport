import com.google.gson.*;
import java.util.*;
import java.util.jar.JarFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Check both real native completion paths and false-positive boundaries. */
public final class RayquazaMegaAdvancementVerification {
    private static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Set<String> calls(MethodNode m){
        Set<String> found=new HashSet<>();
        for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){MethodInsnNode c=(MethodInsnNode)i;found.add(c.owner+"."+c.name);}
        return found;
    }
    private static MethodNode method(ClassNode c,String name){return c.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
    private static ClassNode nativeClass(JarFile jar,String name)throws Exception{
        ClassNode c=new ClassNode();try(var in=jar.getInputStream(jar.getJarEntry(name+".class"))){new ClassReader(in).accept(c,0);}return c;
    }
    public static void main(String[] args)throws Exception{
        JsonObject a=Version13StaticVerification.resource("data/pixelmon_backport/advancements/legends/form_384_mega.json");
        JsonObject display=a.getAsJsonObject("display");
        require(display.getAsJsonObject("icon").get("nbt").getAsString().contains("ndex:384s")&&display.getAsJsonObject("icon").get("nbt").getAsString().contains("form:\"mega\""),"Wrong Rayquaza form icon");
        require(display.get("hidden").getAsBoolean()&&display.get("show_toast").getAsBoolean()&&display.get("announce_to_chat").getAsBoolean(),"Completion visibility/feedback incorrect");
        require(a.getAsJsonObject("criteria").getAsJsonObject("earned").get("trigger").getAsString().equals("minecraft:impossible"),"Capture must not automatically grant Mega achievement");
        require(Version13StaticVerification.resource("assets/pixelmon_backport/lang/zh_cn.json").get("pixelmon_backport.advancement.rayquaza_mega.title").getAsString().equals("凌云九霄"),"Wrong requested title");
        ClassNode award=Version13StaticVerification.node("local/pixelmon/backport/LegendAdvancements");
        Set<String> battle=calls(method(award,"megaRayquaza"));
        require(battle.contains("local/pixelmon/backport/AcquisitionAdvancements.ownedStored")&&battle.stream().anyMatch(s->s.endsWith(".getParticipant")),"Battle must validate real owned storage and player participant");
        boolean simulation=false,ended=false;
        for(AbstractInsnNode i:method(award,"megaRayquaza").instructions)if(i instanceof FieldInsnNode){String name=((FieldInsnNode)i).name;simulation|=name.equals("simulateMode");ended|=name.equals("battleEnded");}
        require(simulation&&ended,"Simulated/ended battles may grant Mega achievement");
        Set<String> field=calls(method(award,"formChanged"));
        require(field.contains("local/pixelmon/backport/FieldMega.active")&&field.contains("local/pixelmon/backport/AcquisitionAdvancements.ownedStored"),"Field must observe active owned native animation");
        MethodNode hook=method(Version13StaticVerification.node("local/pixelmon/backport/mixin/PrimalAdvancementMixin"),"backport$completedMega");
        require(calls(hook).stream().anyMatch(s->s.endsWith(".getReturnValueZ"))&&calls(hook).contains("local/pixelmon/backport/LegendAdvancements.megaRayquaza"),"Canceled or failed native Mega may grant achievement");
        try(JarFile jar=new JarFile(args[0])){
            ClassNode wrapper=nativeClass(jar,"com/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper");
            require(calls(method(wrapper,"megaEvolve")).stream().anyMatch(s->s.endsWith(".setForm")),"Native Mega completion path changed");
            ClassNode query=nativeClass(jar,"com/pixelmonmod/pixelmon/entities/pixelmon/helpers/EvolutionQuery");
            require(query.methods.stream().anyMatch(m->calls(m).contains("com/pixelmonmod/pixelmon/api/pokemon/Pokemon.setForm")),"Field native animation no longer completes Pokemon form");
        }
        System.out.println("PASS 凌云九霄 Mega sprite/title, real native field and battle completion paths, canceled/failed/simulation/NPC/ownership boundaries. Gameplay acceptance remains manual.");
    }
}
