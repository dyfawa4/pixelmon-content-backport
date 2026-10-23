package local.pixelmon.backport.mixin;
import local.pixelmon.backport.AuthorChallenge;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.StatsEffect;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.controller.log.AttackResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=StatsEffect.class,remap=false)
public abstract class AuthorBuffMixin {
 @Shadow public int amount;@Shadow public boolean isUser;
 @Inject(method="applyStatEffect",at=@At("HEAD"),cancellable=true)
 private void backport$buff(PixelmonWrapper user,PixelmonWrapper target,ImmutableAttack attack,CallbackInfoReturnable<AttackResult> cir){if(amount>0&&(isUser||user==target)&&!AuthorChallenge.buff(user))cir.setReturnValue(AttackResult.failed);}
}
