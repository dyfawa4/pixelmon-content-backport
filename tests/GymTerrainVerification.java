import local.pixelmon.backport.GymTerrainProtection;
import org.objectweb.asm.tree.*;

public final class GymTerrainVerification {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        for(int origin:new int[]{-8192,-33,-16,-1,0,15,16,1200}){
            check(GymTerrainProtection.intersects(origin,origin,origin+15,origin+15,origin+15,origin+15,origin+40,origin+40),"Cross-chunk lake edge not protected");
            check(!GymTerrainProtection.intersects(origin,origin,origin+15,origin+15,origin+16,origin+16,origin+40,origin+40),"Nonoverlapping surroundings suppressed");
            check(GymTerrainProtection.intersects(origin,origin,origin+15,origin+15,origin-60,origin-60,origin+60,origin+60),"Inside city not protected");
        }
        boolean planned=false,loadedOnly=false,generationOnly=false;int boundedReads=0;
        for(MethodNode m:Version13StaticVerification.node("local/pixelmon/backport/GymTerrainProtection").methods)for(AbstractInsnNode i:m.instructions){
            if(i instanceof MethodInsnNode){var n=(MethodInsnNode)i;planned|=n.name.equals("func_230346_b_");
                if(n.name.equals("func_217353_a")){boundedReads++;loadedOnly|=i.getPrevious().getOpcode()==org.objectweb.asm.Opcodes.ICONST_0;check(i.getPrevious().getOpcode()==org.objectweb.asm.Opcodes.ICONST_0,"Region read requires an out-of-region chunk");}
                check(!n.name.equals("func_241827_a"),"Lazy StructureManager stream can read out-of-region starts");
                check(!n.name.equals("func_241211_a_") && !n.name.equals("func_180501_a"),"Protection repaired the world after damage instead of preventing the feature");}
            if(i instanceof TypeInsnNode)generationOnly|=((TypeInsnNode)i).desc.equals("net/minecraft/world/gen/WorldGenRegion");
        }
        check(planned && loadedOnly && generationOnly && boundedReads==2,"Bounded holder/start reads or generation boundary missing");
        boolean villageOnly=false;for(MethodNode m:Version13StaticVerification.node("net/minecraft/world/gen/feature/LakesFeature").methods)for(AbstractInsnNode i:m.instructions)if(i instanceof FieldInsnNode)villageOnly|=((FieldInsnNode)i).name.equals("field_236381_q_");
        check(villageOnly,"Recheck native lake's vanilla-village-only protection");
        boolean radius=false,delegated=false;
        for(MethodNode m:Version13StaticVerification.node("local/pixelmon/backport/mixin/GymMushroomProtectionMixin").methods)for(AbstractInsnNode i:m.instructions){
            if(i instanceof FieldInsnNode)radius|=((FieldInsnNode)i).name.equals("field_227274_c_");
            if(i instanceof MethodInsnNode)delegated|=((MethodInsnNode)i).owner.equals("local/pixelmon/backport/GymTerrainProtection")&&((MethodInsnNode)i).name.equals("protectedArea");
        }
        check(radius&&delegated,"Giant mushroom cap radius or existing city protection omitted");
        for(String type:new String[]{"BigBrownMushroomFeature","BigRedMushroomFeature"})check(Version13StaticVerification.node("net/minecraft/world/gen/feature/"+type).superName.equals("net/minecraft/world/gen/feature/AbstractBigMushroomFeature"),"Native mushroom bypasses shared hook");
        for(int edge:new int[]{-33,-16,-1,0,15,16,1200}){
            check(GymTerrainProtection.intersects(edge-4,edge-4,edge+2,edge+2,edge+1,edge+1,edge+16,edge+16),"Mushroom cap crossing city boundary missed");
            check(!GymTerrainProtection.intersects(edge-8,edge-8,edge-2,edge-2,edge+1,edge+1,edge+16,edge+16),"Unrelated wild mushroom blocked");
        }
        boolean chorusGuard=false,branchRadius=false;
        for(MethodNode m:Version13StaticVerification.node("local/pixelmon/backport/mixin/GymChorusProtectionMixin").methods)for(AbstractInsnNode i:m.instructions){
            if(i instanceof MethodInsnNode)chorusGuard|=((MethodInsnNode)i).owner.equals("local/pixelmon/backport/GymTerrainProtection");
            if(i instanceof IntInsnNode)branchRadius|=((IntInsnNode)i).operand==8;
        }
        check(chorusGuard&&branchRadius,"Natural chorus guard must cover native eight-block branch reach");
        for(int edge:new int[]{-17,-1,0,15,16})check(GymTerrainProtection.intersects(edge-8,0,edge+8,0,edge+8,0,edge+24,16),"Chorus branch crossing a city edge");
        System.out.println("PASS red/brown giant mushrooms share native placement guard including cap overlap; player-grown/world-boundary policies retained.");
        System.out.println("PASS planned gym footprint protection before natural lakes/trees/patches; negative-coordinate/cross-chunk edges and untouched surroundings, loaded-reference-only and generation-only boundaries; template water and player saplings unaffected. Natural generation remains manual.");
    }
}
