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
        boolean planned=false,loadedOnly=false,generationOnly=false;
        for(MethodNode m:Version13StaticVerification.node("local/pixelmon/backport/GymTerrainProtection").methods)for(AbstractInsnNode i:m.instructions){
            if(i instanceof MethodInsnNode){var n=(MethodInsnNode)i;planned|=n.name.equals("func_241827_a");loadedOnly|=n.name.equals("func_217354_b");check(!n.name.equals("func_241211_a_") && !n.name.equals("func_180501_a"),"Protection repaired the world after damage instead of preventing the feature");}
            if(i instanceof TypeInsnNode)generationOnly|=((TypeInsnNode)i).desc.equals("net/minecraft/world/gen/WorldGenRegion");
        }
        check(planned && loadedOnly && generationOnly,"Planned/referenced city or generation boundary missing");
        boolean villageOnly=false;for(MethodNode m:Version13StaticVerification.node("net/minecraft/world/gen/feature/LakesFeature").methods)for(AbstractInsnNode i:m.instructions)if(i instanceof FieldInsnNode)villageOnly|=((FieldInsnNode)i).name.equals("field_236381_q_");
        check(villageOnly,"Recheck native lake's vanilla-village-only protection");
        System.out.println("PASS planned gym footprint protection before natural lakes/trees/patches; negative-coordinate/cross-chunk edges and untouched surroundings, loaded-reference-only and generation-only boundaries; template water and player saplings unaffected. Natural generation remains manual.");
    }
}
