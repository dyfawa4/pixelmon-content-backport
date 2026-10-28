package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.Confusion;
import com.pixelmonmod.pixelmon.api.util.helpers.RandomHelper;
import local.pixelmon.backport.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=Confusion.class,remap=false)
public abstract class NpcConfusionRollMixin {
    @Redirect(method="canAttackThisTurn",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/util/helpers/RandomHelper;getRandomChance(F)Z"),require=1)
    private boolean backport$sharedStatus(float chance,PixelmonWrapper user,Attack attack){
        if(AuthorForesight.forecast(user)&&(!user.bc.simulateMode||NpcTurnKnowledge.present(user.bc)))return AuthorForesight.statusChance(user,"confusion",(double)chance*100);
        return RandomHelper.getRandomChance(chance);
    }
}
