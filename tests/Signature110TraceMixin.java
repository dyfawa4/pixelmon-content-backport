package qa.mixin;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.attackModifiers.MultipleHit;import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;import com.pixelmonmod.pixelmon.battles.controller.log.AttackResult;
@Mixin(value=MultipleHit.class,remap=false)
public class Signature110TraceMixin {
 @Shadow private int count;@Shadow private int targetCount;@Shadow private boolean inProgress;
 @Unique private static int traced;
 @Inject(method="applyEffectDuring",at=@At("HEAD")) private void head(PixelmonWrapper user,PixelmonWrapper target,CallbackInfoReturnable<AttackResult> c){if(traced++<16)System.out.println("SIGNATURE_TRACE_HEAD sim="+user.bc.simulateMode+" hits="+targetCount+" progress="+inProgress+" result="+(user.attack.moveResult==null?"null":user.attack.moveResult.fullDamage));}
 @Inject(method="applyEffectDuring",at=@At("RETURN")) private void end(PixelmonWrapper user,PixelmonWrapper target,CallbackInfoReturnable<AttackResult> c){if(traced<17)System.out.println("SIGNATURE_TRACE_END sim="+user.bc.simulateMode+" hits="+targetCount+" count="+count+" result="+(user.attack.moveResult==null?"null":user.attack.moveResult.fullDamage));}
}
