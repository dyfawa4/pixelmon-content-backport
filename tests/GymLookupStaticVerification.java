import java.io.*;
import java.util.*;
import local.pixelmon.backport.GymThemePaths;
import local.pixelmon.backport.mixin.BackportMixinPlugin;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

/** Offline path fixtures and bytecode checks only; no Forge world is started. */
public final class GymLookupStaticVerification {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    static ClassNode node(String name)throws Exception{
        ClassNode n=new ClassNode();try(InputStream in=GymLookupStaticVerification.class.getClassLoader().getResourceAsStream(name+".class")){new ClassReader(in.readAllBytes()).accept(n,0);}return n;
    }
    public static void main(String[] args)throws Exception{
        for(int i=0;i<18;i++){
            String type=GymThemePaths.TYPES[i];
            check(type.equals(GymThemePaths.normalize(GymThemePaths.CHINESE[i])),"Chinese alias "+type);
            for(String ns:new String[]{"pixelmon","pixelmon_backport"})check(type.equals(GymThemePaths.classify(Arrays.asList("Single["+ns+":gyms/"+type+"/gym_"+type+"]"))),"Main gym "+type);
        }
        check("grass".equals(GymThemePaths.classify(Arrays.asList("Single[pixelmon_backport:gyms/dark/gym_dark_house_1]","Single[pixelmon:gyms/grass/gym_grass]"))),"Mixed dark pool must return real grass gym");
        check(GymThemePaths.classify(Arrays.asList("pixelmon_backport:gyms/poison/gym_poison_leader_a]","pixelmon:gyms/rock/gym_rock_house_a]"))==null,"NPCs and houses cannot identify main gym");
        check(GymThemePaths.classify(Arrays.asList("pixelmon:gyms/fire/gym_fire]","pixelmon:gyms/water/gym_water]"))==null,"Ambiguous starts fail closed");
        check(GymThemePaths.poolTypes("pixelmon_backport:gyms/natural/dark/start_pool").equals(new HashSet<>(Arrays.asList("dark","grass"))),"Mixed natural pool dimensions");
        check(GymThemePaths.poolTypes("pixelmon:gyms/fire/start_pool").equals(Collections.singleton("fire")),"Fire pool dimensions");
        check(GymThemePaths.poolTypes("pixelmon:gyms/fire/house").isEmpty(),"Unknown datapack pool cannot imply a type");
        ClassNode search=node("local/pixelmon/backport/GymLocator$StartRequest");boolean startsOnly=false;
        for(MethodNode m:search.methods)for(AbstractInsnNode i:m.instructions){
            if(i instanceof FieldInsnNode){FieldInsnNode f=(FieldInsnNode)i;if(f.owner.equals("net/minecraft/world/chunk/ChunkStatus")){check(f.name.equals("field_222606_b"),"Lookup must not generate FULL cities");startsOnly=true;}}
        }
        check(startsOnly,"Lookup structure-start status missing");
        boolean async=false,release=false;
        for(MethodNode m:search.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){
            var c=(MethodInsnNode)i;
            check(!c.name.equals("func_217348_a")&&!c.name.equals("func_212849_a_"),"Lookup must never managed-block the server thread");
            async|=c.name.equals("backport$requestChunk");release|=m.name.equals("close")&&c.name.equals("func_217222_b");
        }
        check(async&&release,"Async start request must release its ticket on cancellation");
        ClassNode jei=node("local/pixelmon/backport/client/BackportJeiPlugin");boolean black=false;
        for(MethodNode m:jei.methods)if(m.name.equals("information"))for(AbstractInsnNode i:m.instructions)if(i instanceof FieldInsnNode){FieldInsnNode f=(FieldInsnNode)i;if(f.owner.equals("net/minecraft/util/text/TextFormatting") && f.name.equals("BLACK"))black=true;}
        check(black,"JEI body must be black");
        // Verify the native reward path offline instead of simulating a victory.
        ClassNode trainer=node("com/pixelmonmod/pixelmon/entities/npcs/NPCTrainer");
        boolean rewards=false,copy=false,register=false,deserialize=false;
        for(MethodNode m:trainer.methods)for(AbstractInsnNode i:m.instructions){
            if(m.name.equals("loseBattle") && i instanceof FieldInsnNode && ((FieldInsnNode)i).name.equals("winnings"))rewards=true;
            if(m.name.equals("loseBattle") && i instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)i;
                copy|=call.owner.equals("net/minecraft/item/ItemStack") && call.name.equals("func_77946_l");
                register|=call.owner.endsWith("/DropItemQueryList") && call.name.equals("register");
            }
            if(i instanceof LdcInsnNode && "WinningsTag".equals(((LdcInsnNode)i).cst))deserialize=true;
        }
        check(rewards && copy && register && deserialize,"Native trainer must read reward NBT and offer copied rewards after defeat");
        ClassNode maps=node("GymTestMaps");boolean optional=false;
        for(MethodNode m:maps.methods)if(m.name.equals("tick"))for(AbstractInsnNode i:m.instructions)if(i instanceof TypeInsnNode && i.getOpcode()==org.objectweb.asm.Opcodes.INSTANCEOF)optional|=((TypeInsnNode)i).desc.equals("net/minecraft/world/gen/feature/jigsaw/FeatureJigsawPiece");
        check(optional,"Preview must distinguish optional native features from failed buildings");
        if(GymLookupStaticVerification.class.getClassLoader().getResource("com/chaosthedude/explorerscompass/items/ExplorersCompassItem.class")!=null){
            ClassNode item=node("com/chaosthedude/explorerscompass/items/ExplorersCompassItem");
            boolean x=false,z=false,state=false;
            for(MethodNode m:item.methods)if(m.name.equals("setFound"))for(AbstractInsnNode i:m.instructions)if(i instanceof LdcInsnNode){
                Object v=((LdcInsnNode)i).cst;x|="FoundX".equals(v);z|="FoundZ".equals(v);state|="State".equals(v);
            }
            check(x && z && state,"Installed compass must store successful coordinates and state");
            ClassNode angle=node("com/chaosthedude/explorerscompass/ExplorersCompass$1");x=z=false;
            for(MethodNode m:angle.methods)if(m.name.equals("getAngle"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){
                String name=((MethodInsnNode)i).name;x|=name.equals("getFoundStructureX");z|=name.equals("getFoundStructureZ");
            }
            check(x && z,"Installed needle angle must use the stored successful coordinates");
            ClassNode adapter=node("local/pixelmon/backport/mixin/CompassGymSearchMixin");boolean stop=false,keep=false;
            for(MethodNode m:adapter.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode && ((MethodInsnNode)i).name.equals("backport$stale")){
                stop|=m.name.equals("backport$stopSupersededSearch");keep|=m.name.equals("backport$keepCurrentTarget");
            }
            check(stop && keep,"Superseded attribute searches must neither continue nor overwrite current compass results");
        }
        check(!new BackportMixinPlugin().shouldApplyMixin("com.chaosthedude.explorerscompass.missing.Target","test"),"Absent optional compass must be skipped");
        System.out.println("PASS 18 gym aliases, real-theme filtering, bounded start-only lookup, black JEI body, native badge reward path, optional features and optional-mod absence");
    }
}
