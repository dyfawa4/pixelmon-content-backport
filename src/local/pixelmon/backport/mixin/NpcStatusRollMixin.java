package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.*;
import com.pixelmonmod.pixelmon.api.util.helpers.RandomHelper;
import local.pixelmon.backport.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value={Paralysis.class,Freeze.class},remap=false)
public abstract class NpcStatusRollMixin {
    @Redirect(method="canAttackThisTurn",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/util/helpers/RandomHelper;getRandomChance(I)Z"),require=1)
    private boolean backport$sharedStatus(int chance,PixelmonWrapper user,Attack attack){
        if(AuthorForesight.forecast(user)&&(!user.bc.simulateMode||NpcTurnKnowledge.present(user.bc)))
            return AuthorForesight.statusChance(user,(Object)this instanceof Paralysis?"paralysis":"freeze",chance);
        return RandomHelper.getRandomChance(chance);
    }
}
