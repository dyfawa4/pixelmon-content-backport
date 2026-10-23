package local.pixelmon.backport.mixin;
import local.pixelmon.backport.AuthorChallenge;
import com.pixelmonmod.pixelmon.battles.status.ProtectVariation;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=ProtectVariation.class,remap=false)
public abstract class AuthorProtectMixin {
 @Redirect(method="applyEffect",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/util/helpers/RandomHelper;getRandomChance(F)Z"))
 private boolean backport$protect(float chance,PixelmonWrapper user,PixelmonWrapper target){return com.pixelmonmod.pixelmon.api.util.helpers.RandomHelper.getRandomChance(AuthorChallenge.affected(user)?chance*.8f:chance);}
}
