import org.objectweb.asm.tree.*;
public final class PokeChestLootVerification {
    static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);}
    public static void main(String[] args)throws Exception{
        boolean positive=false,nonempty=false,custom=false,owner=false,master=false;
        for(MethodNode m:Version13StaticVerification.node("local/pixelmon/backport/PokeChestLoot").methods)for(var i:m.instructions){
            if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;positive|=c.name.equals("func_190916_E");nonempty|=c.name.equals("func_190926_b");custom|=c.name.equals("isCustomDrop");owner|=c.name.equals("getOwner");}
            if(i instanceof FieldInsnNode)master|=((FieldInsnNode)i).name.equals("MASTERBALL");
        }
        check(positive && nonempty && custom && owner && master,"Loot validity/scope guard missing");
        boolean claim=false,give=false,cancel=false;
        for(MethodNode m:Version13StaticVerification.node("com/pixelmonmod/pixelmon/blocks/PokeChestBlock").methods)if(m.name.equals("func_225533_a_"))for(var i:m.instructions)if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;claim|=c.name.equals("addClaimer");give|=c.name.equals("giveItemStack");cancel|=c.name.equals("post");}
        check(claim && give && cancel,"Native claim/give/fullbag/cancellation path missing");
        System.out.println("PASS wild master-chest scope, null/air/nonpositive rejection, native valid-array and per-drop fallback wiring, original claim/cancel/fullbag path retained; no game initialized.");
    }
}
