package local.pixelmon.backport.mixin;
import local.pixelmon.backport.AuthorChallenge;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.StatsEffectPost;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=StatsEffectPost.class,remap=false)
public abstract class AuthorBuffPostMixin {
 @Shadow public int amount;
 @Inject(method="applyEffectAfterAllTargets",at=@At("HEAD"),cancellable=true)
 private void backport$buff(PixelmonWrapper user,CallbackInfo ci){if(amount>0&&!AuthorChallenge.buff(user))ci.cancel();}
}
