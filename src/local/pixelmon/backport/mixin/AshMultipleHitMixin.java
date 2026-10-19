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
    // Apply before the original raid-shield exception; preserve native per-hit resolution.
    @Inject(method="applyEffectDuring",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;isRaidPokemon()Z"))
    private void backport$ashOriginHits(PixelmonWrapper user,PixelmonWrapper target,CallbackInfoReturnable<AttackResult> cir){
        if(local.pixelmon.backport.AshWaterBoost.eligible(user)&&!user.usingZ&&"Water Shuriken".equals(user.attack.getMove().getAttackName()))targetCount=3;
    }
    @ModifyArg(method="applyEffectDuring",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/species/Stats;isForm([Ljava/lang/String;)Z"),index=0)
    private String[] backport$waterShurikenHits(String[] forms){
        if(forms.length==1 && "ash".equals(forms[0]))return new String[]{"ash",PermanentAsh.FORM,PermanentAsh.MEGA};
        return forms;
    }
}
