package local.pixelmon.backport.mixin;
import local.pixelmon.backport.PermanentAsh;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.attackModifiers.MultipleHit;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.controller.log.AttackResult;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=MultipleHit.class,remap=false)
public abstract class AshMultipleHitMixin {
    @Shadow private int targetCount;
    @Shadow private boolean inProgress;
    // Apply before the original raid-shield exception; preserve native per-hit resolution.
    @Inject(method="applyEffectDuring",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;isRaidPokemon()Z"))
    private void backport$ashOriginHits(PixelmonWrapper user,PixelmonWrapper target,CallbackInfoReturnable<AttackResult> cir){
        if(local.pixelmon.backport.AshWaterBoost.eligible(user)&&!user.usingZ&&"Water Shuriken".equals(user.attack.getMove().getAttackName()))targetCount=user.bc.simulateMode?1:3;
    }
    // Native simulation resolves one hit and multiplies it at the end. Forcing
    // targetCount=3 disabled that aggregate path and left the preview at one hit.
    @ModifyConstant(method="applyEffectDuring",constant=@Constant(doubleValue=3.168D),require=2)
    private double backport$ashExpectedHits(double nativeHits,PixelmonWrapper user,PixelmonWrapper target){
        return !target.isRaidPokemon()&&local.pixelmon.backport.AshWaterBoost.eligible(user)&&!user.usingZ&&"Water Shuriken".equals(user.attack.getMove().getAttackName())?3:nativeHits;
    }
    @Inject(method="applyEffectDuring",at=@At("RETURN"))
    private void backport$aggregate(PixelmonWrapper user,PixelmonWrapper target,CallbackInfoReturnable<AttackResult> cir){
        if(user.bc.simulateMode&&!inProgress&&targetCount==1&&user.attack!=null&&user.attack.moveResult!=null)
            local.pixelmon.backport.NpcSignaturePreview.record(user,user.attack.moveResult);
    }
    @ModifyArg(method="applyEffectDuring",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/species/Stats;isForm([Ljava/lang/String;)Z"),index=0)
    private String[] backport$waterShurikenHits(String[] forms){
        if(forms.length==1 && "ash".equals(forms[0]))return new String[]{"ash",PermanentAsh.FORM,PermanentAsh.MEGA};
        return forms;
    }
}
