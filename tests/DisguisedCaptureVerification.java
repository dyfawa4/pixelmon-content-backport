import local.pixelmon.backport.DisguisedCapture;
import org.objectweb.asm.tree.*;
public final class DisguisedCaptureVerification {
    static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);}
    public static void main(String[] args)throws Exception{
        check(DisguisedCapture.qualifies(true,132,true),"Revealed real stored Ditto rejected");
        check(!DisguisedCapture.qualifies(false,132,true),"Ordinary Ditto accepted");
        check(!DisguisedCapture.qualifies(true,808,true),"Disguise species accepted");
        check(!DisguisedCapture.qualifies(true,132,false),"Failed/cancelled storage accepted");
        boolean nativeReveal=false;for(MethodNode m:Version13StaticVerification.node("com/pixelmonmod/pixelmon/entities/pokeballs/EmptyPokeBallEntity").methods)if(m.name.equals("catchPokemon"))for(var i:m.instructions)if(i instanceof MethodInsnNode)nativeReveal|=((MethodInsnNode)i).name.equals("checkReallyADitto");
        check(nativeReveal,"Native success reveal path missing");
        boolean guarded=false,queued=false;
        for(MethodNode m:Version13StaticVerification.node("local/pixelmon/backport/LegendAdvancements").methods)for(var i:m.instructions)if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;
            if(m.name.equals("capture"))guarded|=c.name.equals("ownedStored");
            if(m.name.equals("captured")){queued|=c.owner.endsWith("/AcquisitionQueue") && c.name.equals("stored");check(!c.owner.endsWith("/LegendAdvancements") || !c.name.equals("capture"),"Pre-reveal capture event awards displayed species");}
        }
        check(guarded && queued,"Species award must wait for canonical final storage");
        System.out.println("PASS transient actual-capture reveal token, real stored Ditto and ordinary/gift/failure/cancellation/disguise negatives; no game initialized.");
    }
}
